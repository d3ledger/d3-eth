package contract;

import java.math.BigInteger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.2.0.
 */
public class Failer extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b5061011f806100206000396000f3fe608060405260043610601c5760003560e01c8063a9059cbb14606c575b60408051600160e51b62461bcd02815260206004820152601360248201527f657468207472616e736665722072657665727400000000000000000000000000604482015290519081900360640190fd5b348015607757600080fd5b5060a160048036036040811015608c57600080fd5b506001600160a01b03813516906020013560a3565b005b60408051600160e51b62461bcd02815260206004820152601660248201527f4552432d3230207472616e736665722072657665727400000000000000000000604482015290519081900360640190fdfea165627a7a723058203c10f98f9b97bc45bcb7ce6e7a022f989e5331b5e81bb9377a3abe6e5c9d66150029";

    public static final String FUNC_TRANSFER = "transfer";

    @Deprecated
    protected Failer(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Failer(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Failer(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Failer(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public void transfer(String param0, BigInteger param1) {
        throw new RuntimeException("cannot call constant function with void return type");
    }

    @Deprecated
    public static Failer load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Failer(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Failer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Failer(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Failer load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Failer(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Failer load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Failer(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Failer> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Failer.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Failer> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Failer.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<Failer> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(Failer.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<Failer> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(Failer.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }
}
