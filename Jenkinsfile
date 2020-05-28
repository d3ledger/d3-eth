
pipeline {
  environment {
    DOCKER_NETWORK = ''
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '20'))
    timestamps()
    disableConcurrentBuilds()
  }
  agent {
    label 'd3-build-agent'
  }
  stages {
    stage('Tests') {
      steps {
        script {
          tmp = docker.image("gradle:4.10.2-jdk8-slim")
          env.WORKSPACE = pwd()

          DOCKER_NETWORK = "${env.CHANGE_ID}-${env.GIT_COMMIT}-${BUILD_NUMBER}"
          writeFile file: ".env", text: "SUBNET=${DOCKER_NETWORK}"
          withCredentials([usernamePassword(credentialsId: 'bot-soranet-ro', usernameVariable: 'login', passwordVariable: 'password')]) {
            sh "docker login docker.soramitsu.co.jp -u ${login} -p '${password}'"
          }

          withCredentials([usernamePassword(credentialsId: 'nexus-d3-docker', usernameVariable: 'login', passwordVariable: 'password')]) {
            sh "docker login nexus.iroha.tech:19002 -u ${login} -p '${password}'"
          }

          sh "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml pull"
          sh(returnStdout: true, script: "docker-compose -f deploy/docker-compose.yml -f deploy/docker-compose.ci.yml up --build -d")


          iC = docker.image("openjdk:8-jdk")
          iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb' -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp") {
            sh "./gradlew dependencies"
            sh "./gradlew test --info"
            // We need this to test containers
            sh "./gradlew dockerfileCreate"
            sh "./gradlew compileIntegrationTestKotlin --info"
            sh "./gradlew integrationTest --info"
            sh "./gradlew d3TestReport"
          }
          if (env.BRANCH_NAME == 'develop') {
            iC.inside("--network='d3-${DOCKER_NETWORK}' -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'") {
              withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]){
                sh(script: "./gradlew sonarqube -x test --configure-on-demand \
                  -Dsonar.links.ci=${BUILD_URL} \
                  -Dsonar.github.pullRequest=${env.CHANGE_ID} \
                  -Dsonar.github.disableInlineComments=true \
                  -Dsonar.host.url=https://sonar.soramitsu.co.jp \
                  -Dsonar.login=${SONAR_TOKEN} \
                  ")
                }
            }
          }
          publishHTML (target: [
                allowMissing: false,
                alwaysLinkToLastBuild: false,
                keepAll: true,
                reportDir: 'build/reports',
                reportFiles: 'd3-test-report.html',
                reportName: "D3 test report"])
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
        }
      }
    }
    stage('Build and push docker images') {
      steps {
        script {
          if (env.BRANCH_NAME ==~ /(master|develop|reserved)/ || env.TAG_NAME) {
                withCredentials([usernamePassword(credentialsId: 'bot-soranet-rw', usernameVariable: 'login', passwordVariable: 'password')]) {
                  TAG = env.TAG_NAME ? env.TAG_NAME : env.BRANCH_NAME
                  iC = docker.image("gradle:4.10.2-jdk8-slim")
                  iC.inside(" -e JVM_OPTS='-Xmx3200m' -e TERM='dumb'"+
                  " -v /var/run/docker.sock:/var/run/docker.sock -v /tmp:/tmp"+
                  " -e DOCKER_REGISTRY_URL='https://docker.soramitsu.co.jp'"+
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
  post {
      cleanup {
          cleanWs()
      }
  }
}
