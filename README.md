# D3 Ethereum

## How to run notary application and services in Ethereum main net
1) Run [common services](https://github.com/d3ledger/notary#how-to-run-notary-application-and-services-in-ethereum-main-net).
2) Provide ethereum passwords `configs/eth/ethereum_password_mainnet.properties` (ask someone from maintainers team about the format)
3) Deploy Ethereum master contract and relay registry contract, provide notary ethereum accounts `./gradlew runPreDeployEthereum --args="0x6826d84158e516f631bbf14586a9be7e255b2d23"`
4) Run services with `docker-compose -f deploy/docker-compose.eth.yml up`

Great! So now you can move on and connect frontend application (check back-office repo in d3ledger)

## Ethereum passwords
Passwords for Ethereum network may be set in 3 different ways:

1) Using `eth/ethereum_password.properties` file.
2) Using environment variables(`ETH_CREDENTIALS_PASSWORD`, `ETH_NODE_LOGIN` and `ETH_NODE_PASSWORD`).
3) Using command line arguments. For example `./gradlew runEthDeposit -PcredentialsPassword=test -PnodeLogin=login -PnodePassword=password`

Configurations have the following priority:

Command line args > Environment variables > Properties file

## Testing Ethereum
There is a dedicated endpoint for testing purposes. Visit [Swagger](http://127.0.0.1:18982/apidocs) for more details.
