# D3 Ethereum

## How to run notary application and services in Ethereum main net
1) Run common services
2) Provide ethereum passwords `configs/eth/ethereum_password_mainnet.properties` (ask someone from maintainers team about the format)
3) Deploy Ethereum master contract and relay registry contract, provide notary ethereum accounts `./gradlew runPreDeployEthereum --args="0x6826d84158e516f631bbf14586a9be7e255b2d23"` 
4) Run deposit service `PROFILE=mainnet ./gradlew runEthDeposit`
5) Run registration service `PROFILE=mainnet ./gradlew runEthRegistration`
6) Run withdrawal service `PROFILE=mainnet ./gradlew runWithdrawal`
7) Deploy relay smart contract (one relay per one client registration) `PROFILE=mainnet ./gradlew runDeployRelay`. Ensure relay is deployed on etherscan.io

Great! So now you can move on and connect frontend application (check back-office repo in d3ledger)

## Ethereum passwords
Passwords for Ethereum network may be set in 3 different ways:

1) Using `eth/ethereum_password.properties` file.
2) Using environment variables(`ETH_CREDENTIALS_PASSWORD`, `ETH_NODE_LOGIN` and `ETH_NODE_PASSWORD`).
3) Using command line arguments. For example `./gradlew runEthDeposit -PcredentialsPassword=test -PnodeLogin=login -PnodePassword=password`

Configurations have the following priority:

Command line args > Environment variables > Properties file