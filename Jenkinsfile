
pipeline {
  environment {
    DOCKER_NETWORK = ''
  }
  options {
    skipDefaultCheckout()
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timestamps()
  }
  agent any
  stages {
    stage('Stop same job builds') {
      agent { label 'master' }
      steps {
        script {
          def scmVars = checkout scm
          // need this for develop->master PR cases
          // CHANGE_BRANCH is not defined if this is a branch build
          try {
            scmVars.CHANGE_BRANCH_LOCAL = scmVars.CHANGE_BRANCH
          }
          catch (MissingPropertyException e) {
          }
          if (scmVars.GIT_LOCAL_BRANCH != "develop" && scmVars.CHANGE_BRANCH_LOCAL != "develop") {
            def builds = load ".jenkinsci/cancel-builds-same-job.groovy"
            builds.cancelSameJobBuilds()
          }
        }
      }
    }
    stage('Tests') {
      agent { label 'd3-build-agent' }
      steps {
        script {
          def scmVars = checkout scm
          tmp = docker.image("gradle:4.10.2-jdk8-slim")
          env.WORKSPACE = pwd()

          DOCKER_NETWORK = "${scmVars.CHANGE_ID}-${scmVars.GIT_COMMIT}-${BUILD_NUMBER}"
          writeFile file: ".env", text: "SUBNET=${DOCKER_NETWORK}"
          withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
            sh "docker login nexus.iroha.tech:19002 -u ${login} -p '${password}'"

            sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml pull"
            sh(returnStdout: true, script: "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml up --build -d")
            }

          iC = docker.image("openjdk:8-jdk")
          iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb' -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp") {
            sh "./gradlew dependencies"
            sh "./gradlew test --info"
            // We need this to test containers
            sh "./gradlew eth-withdrawal:shadowJar"
            sh "./gradlew dockerfileCreate"
            
            sh "./gradlew compileIntegrationTestKotlin --info"
            sh "./gradlew integrationTest --info"
          }
          // scan smartcontracts only on pull requests to master
          try {
            if (env.CHANGE_TARGET == "master") {
              docker.image("mythril/myth").inside("--entrypoint=''") {
                sh "echo 'Smart contracts scan results' > mythril.txt"
                // using mythril to scan all solidity files
                sh "find . -name '*.sol' -exec myth --execution-timeout 900 --create-timeout 900 -x {} \\; | tee mythril.txt"
              }
              // save results as a build artifact
              zip archive: true, dir: '', glob: 'mythril.txt', zipFile: 'smartcontracts-scan-results.zip'
            }
          }
          catch(MissingPropertyException e) { }
          
        }
      }
      post {
        always {
          junit allowEmptyResults: true, keepLongStdio: true, testResults: 'build/test-results/**/*.xml'
        }
        cleanup {
          sh "mkdir -p build-logs"
          sh """#!/bin/bash
            while read -r LINE; do \
              docker logs \$(echo \$LINE | cut -d ' ' -f1) | gzip -6 > build-logs/\$(echo \$LINE | cut -d ' ' -f2).log.gz; \
            done < <(docker ps --filter "network=d3-${DOCKER_NETWORK}" --format "{{.ID}} {{.Names}}")
          """
          
          sh "tar -zcvf build-logs/notaryEthIntegrationTest.gz -C notary-eth-integration-test/build/reports/tests integrationTest || true"
          archiveArtifacts artifacts: 'build-logs/*.gz'
          sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml down"
          cleanWs()
        }
      }
    }

    stage('Build and push docker images') {
      agent { label 'd3-build-agent' }
      steps {
        script {
          def scmVars = checkout scm
          if (env.BRANCH_NAME ==~ /(master|develop|reserved)/ || env.TAG_NAME) {
                withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
                  TAG = env.TAG_NAME ? env.TAG_NAME : env.BRANCH_NAME
                  iC = docker.image("gradle:4.10.2-jdk8-slim")
                  iC.inside(" -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'"+
                  " -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp"+
                  " -e DOCKER_REGISTRY_URL='https://nexus.iroha.tech:19002'"+
                  " -e DOCKER_REGISTRY_USERNAME='${login}'"+
                  " -e DOCKER_REGISTRY_PASSWORD='${password}'"+
                  " -e TAG='${TAG}'") {
                    sh "gradle shadowJar"
                    sh "gradle dockerPush"
                  }
                 }
              }
        }
      }
    }
  }
}
