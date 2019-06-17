package contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.DynamicArray;
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
public class RelayRegistry extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50610023336001600160e01b0361002816565b610067565b60005460ff161561003857600080fd5b6000805460ff196001600160a01b0390931661010002610100600160a81b031990911617919091166001179055565b6104d8806100766000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c806305ea0de01461005157806374f1e496146100d3578063a10cda9914610149578063c4d66de81461018b575b600080fd5b6100d16004803603604081101561006757600080fd5b6001600160a01b03823516919081019060408101602082013564010000000081111561009257600080fd5b8201836020820111156100a457600080fd5b803590602001918460208302840111640100000000831117156100c657600080fd5b5090925090506101b1565b005b6100f9600480360360208110156100e957600080fd5b50356001600160a01b0316610268565b60408051602080825283518183015283519192839290830191858101910280838360005b8381101561013557818101518382015260200161011d565b505050509050019250505060405180910390f35b6101776004803603604081101561015f57600080fd5b506001600160a01b0381358116916020013516610313565b604080519115158252519081900360200190f35b6100d1600480360360208110156101a157600080fd5b50356001600160a01b03166103da565b60005461010090046001600160a01b031633146101cd57600080fd5b6001600160a01b038316600090815260016020526040902054156101f057600080fd5b6001600160a01b0383166000908152600160205260409020610213908383610419565b50818160405180838360200280828437604051920182900382209450506001600160a01b03871692507fe7350f93df43f07cf9bb430c2fbd1e1b3dd1c564cb0656cb069aa29376cdb1d49150600090a3505050565b60606001600160a01b03821661027d57600080fd5b6001600160a01b03821660009081526001602052604090205461029f57600080fd5b6001600160a01b0382166000908152600160209081526040918290208054835181840281018401909452808452909183018282801561030757602002820191906000526020600020905b81546001600160a01b031681526001909101906020018083116102e9575b50505050509050919050565b6001600160a01b038216600090815260016020526040812054610338575060016103d4565b6001600160a01b038316600090815260016020526040902054156103d05760005b6001600160a01b0384166000908152600160205260409020548110156103ce576001600160a01b038416600090815260016020526040902080548290811061039d57fe5b6000918252602090912001546001600160a01b03848116911614156103c65760019150506103d4565b600101610359565b505b5060005b92915050565b60005460ff16156103ea57600080fd5b6000805460ff196001600160a01b0390931661010002610100600160a81b031990911617919091166001179055565b82805482825590600052602060002090810192821561046c579160200282015b8281111561046c5781546001600160a01b0319166001600160a01b03843516178255602090920191600190910190610439565b5061047892915061047c565b5090565b6104a091905b808211156104785780546001600160a01b0319168155600101610482565b9056fea265627a7a72305820074e3b118d655b983e3fd90f23b1ebd1ca8a3ff9588545c2b51fce23fa9c35bb64736f6c63430005090032";

    public static final String FUNC_ADDNEWRELAYADDRESS = "addNewRelayAddress";

    public static final String FUNC_GETWHITELISTBYRELAY = "getWhiteListByRelay";

    public static final String FUNC_ISWHITELISTED = "isWhiteListed";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final Event ADDNEWRELAY_EVENT = new Event("AddNewRelay", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {}, new TypeReference<DynamicArray<Address>>(true) {}));
    ;

    @Deprecated
    protected RelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected RelayRegistry(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected RelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected RelayRegistry(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> addNewRelayAddress(String relay, List<String> whiteList) {
        final Function function = new Function(
                FUNC_ADDNEWRELAYADDRESS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.Address>(
                        org.web3j.abi.datatypes.Address.class,
                        org.web3j.abi.Utils.typeMap(whiteList, org.web3j.abi.datatypes.Address.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<List> getWhiteListByRelay(String relay) {
        final Function function = new Function(FUNC_GETWHITELISTBYRELAY, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Address>>() {}));
        return new RemoteCall<List>(
                new Callable<List>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List call() throws Exception {
                        List<Type> result = (List<Type>) executeCallSingleValueReturn(function, List.class);
                        return convertToNative(result);
                    }
                });
    }

    public RemoteCall<Boolean> isWhiteListed(String relay, String who) {
        final Function function = new Function(FUNC_ISWHITELISTED, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(relay), 
                new org.web3j.abi.datatypes.Address(who)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    public RemoteCall<TransactionReceipt> initialize(String owner) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(owner)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<AddNewRelayEventResponse> getAddNewRelayEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDNEWRELAY_EVENT, transactionReceipt);
        ArrayList<AddNewRelayEventResponse> responses = new ArrayList<AddNewRelayEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AddNewRelayEventResponse> addNewRelayEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AddNewRelayEventResponse>() {
            @Override
            public AddNewRelayEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDNEWRELAY_EVENT, log);
                AddNewRelayEventResponse typedResponse = new AddNewRelayEventResponse();
                typedResponse.log = log;
                typedResponse.relayAddress = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.whiteList = (byte[]) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AddNewRelayEventResponse> addNewRelayEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDNEWRELAY_EVENT));
        return addNewRelayEventFlowable(filter);
    }

    @Deprecated
    public static RelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static RelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new RelayRegistry(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new RelayRegistry(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static RelayRegistry load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new RelayRegistry(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RelayRegistry.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(RelayRegistry.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<RelayRegistry> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(RelayRegistry.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public static class AddNewRelayEventResponse {
        public Log log;

        public String relayAddress;

        public byte[] whiteList;
    }
}
