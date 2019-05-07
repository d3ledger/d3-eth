package contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
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
public class OwnedUpgradeabilityProxy extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b506100203361002560201b60201c565b61005a565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e657200000000000000008152905190819003601801902055565b6104fd806100696000396000f3fe60806040526004361061004a5760003560e01c8063025313a21461008e5780633659cfe6146100bf5780634f1ef286146100f45780635c60da1b146101aa578063f1739cae146101bf575b60006100546101f2565b90506001600160a01b03811661006957600080fd5b60405136600082376000803683855af43d806000843e81801561008a578184f35b8184fd5b34801561009a57600080fd5b506100a3610215565b604080516001600160a01b039092168252519081900360200190f35b3480156100cb57600080fd5b506100f2600480360360208110156100e257600080fd5b50356001600160a01b031661024b565b005b6100f26004803603604081101561010a57600080fd5b6001600160a01b03823516919081019060408101602082013564010000000081111561013557600080fd5b82018360208201111561014757600080fd5b8035906020019184600183028401116401000000008311171561016957600080fd5b91908080601f01602080910402602001604051908101604052809392919081815260200183838082843760009201919091525092955061027c945050505050565b3480156101b657600080fd5b506100a36101f2565b3480156101cb57600080fd5b506100f2600480360360208110156101e257600080fd5b50356001600160a01b0316610361565b60008060405180806104b160219139604051908190036021019020549392505050565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e65720000000000000000815290519081900360180190205490565b610253610215565b6001600160a01b0316336001600160a01b03161461027057600080fd5b610279816103ed565b50565b610284610215565b6001600160a01b0316336001600160a01b0316146102a157600080fd5b6102aa8261024b565b6000306001600160a01b031634836040518082805190602001908083835b602083106102e75780518252601f1990920191602091820191016102c8565b6001836020036101000a03801982511681845116808217855250505050505090500191505060006040518083038185875af1925050503d8060008114610349576040519150601f19603f3d011682016040523d82523d6000602084013e61034e565b606091505b505090508061035c57600080fd5b505050565b610369610215565b6001600160a01b0316336001600160a01b03161461038657600080fd5b6001600160a01b03811661039957600080fd5b7f5a3e66efaa1e445ebd894728a69d6959842ea1e97bd79b892797106e270efcd96103c2610215565b604080516001600160a01b03928316815291841660208301528051918290030190a161027981610459565b60006103f76101f2565b9050816001600160a01b0316816001600160a01b0316141561041857600080fd5b6104218261048e565b6040516001600160a01b038316907fbc7cd75a20ee27fd9adebab32041f755214dbc6bffa90cc0225b39da2e5c2d3b90600090a25050565b604080517f636f6d2e64336c65646765722e70726f78792e6f776e657200000000000000008152905190819003601801902055565b600060405180806104b16021913960405190819003602101902092909255505056fe636f6d2e64336c65646765722e70726f78792e696d706c656d656e746174696f6ea165627a7a7230582066cab2f2682589478ba6face0abef70461e19728a742d6e3fd7836d8efa673c00029";

    public static final String FUNC_PROXYOWNER = "proxyOwner";

    public static final String FUNC_UPGRADETO = "upgradeTo";

    public static final String FUNC_UPGRADETOANDCALL = "upgradeToAndCall";

    public static final String FUNC_IMPLEMENTATION = "implementation";

    public static final String FUNC_TRANSFERPROXYOWNERSHIP = "transferProxyOwnership";

    public static final Event PROXYOWNERSHIPTRANSFERRED_EVENT = new Event("ProxyOwnershipTransferred", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}, new TypeReference<Address>() {}));
    ;

    public static final Event UPGRADED_EVENT = new Event("Upgraded", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}));
    ;

    @Deprecated
    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected OwnedUpgradeabilityProxy(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<String> proxyOwner() {
        final Function function = new Function(FUNC_PROXYOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> upgradeTo(String implementation) {
        final Function function = new Function(
                FUNC_UPGRADETO, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(implementation)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> upgradeToAndCall(String implementation, byte[] data, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_UPGRADETOANDCALL, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(implementation), 
                new org.web3j.abi.datatypes.DynamicBytes(data)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<String> implementation() {
        final Function function = new Function(FUNC_IMPLEMENTATION, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> transferProxyOwnership(String newOwner) {
        final Function function = new Function(
                FUNC_TRANSFERPROXYOWNERSHIP, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(newOwner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<ProxyOwnershipTransferredEventResponse> getProxyOwnershipTransferredEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(PROXYOWNERSHIPTRANSFERRED_EVENT, transactionReceipt);
        ArrayList<ProxyOwnershipTransferredEventResponse> responses = new ArrayList<ProxyOwnershipTransferredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            ProxyOwnershipTransferredEventResponse typedResponse = new ProxyOwnershipTransferredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<ProxyOwnershipTransferredEventResponse> proxyOwnershipTransferredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, ProxyOwnershipTransferredEventResponse>() {
            @Override
            public ProxyOwnershipTransferredEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(PROXYOWNERSHIPTRANSFERRED_EVENT, log);
                ProxyOwnershipTransferredEventResponse typedResponse = new ProxyOwnershipTransferredEventResponse();
                typedResponse.log = log;
                typedResponse.previousOwner = (String) eventValues.getNonIndexedValues().get(0).getValue();
                typedResponse.newOwner = (String) eventValues.getNonIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<ProxyOwnershipTransferredEventResponse> proxyOwnershipTransferredEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(PROXYOWNERSHIPTRANSFERRED_EVENT));
        return proxyOwnershipTransferredEventFlowable(filter);
    }

    public List<UpgradedEventResponse> getUpgradedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(UPGRADED_EVENT, transactionReceipt);
        ArrayList<UpgradedEventResponse> responses = new ArrayList<UpgradedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            UpgradedEventResponse typedResponse = new UpgradedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.implementation = (String) eventValues.getIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, UpgradedEventResponse>() {
            @Override
            public UpgradedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(UPGRADED_EVENT, log);
                UpgradedEventResponse typedResponse = new UpgradedEventResponse();
                typedResponse.log = log;
                typedResponse.implementation = (String) eventValues.getIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<UpgradedEventResponse> upgradedEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(UPGRADED_EVENT));
        return upgradedEventFlowable(filter);
    }

    @Deprecated
    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static OwnedUpgradeabilityProxy load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new OwnedUpgradeabilityProxy(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<OwnedUpgradeabilityProxy> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(OwnedUpgradeabilityProxy.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class ProxyOwnershipTransferredEventResponse {
        public Log log;

        public String previousOwner;

        public String newOwner;
    }

    public static class UpgradedEventResponse {
        public Log log;

        public String implementation;
    }
}
