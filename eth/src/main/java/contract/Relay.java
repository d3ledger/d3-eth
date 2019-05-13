package contract;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint256;
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
public class Relay extends Contract {
    private static final String BINARY = "608060405234801561001057600080fd5b50604051602080610b458339810180604052602081101561003057600080fd5b505161004281610048602090811b901c565b506100a1565b60005460ff161561005857600080fd5b6000805460018054610100610100600160a81b03199093166001600160a01b039586168402179283049094166001600160a01b031990941693909317835560ff19169091179055565b610a95806100b06000396000f3fe60806040526004361061003f5760003560e01c80636ff3d9f41461007f578063c4d66de8146100b4578063eea29e3e146100e7578063f619ef13146102cb575b361561004a57600080fd5b6040805133815290517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a499181900360200190a1005b34801561008b57600080fd5b506100b2600480360360208110156100a257600080fd5b50356001600160a01b03166104a3565b005b3480156100c057600080fd5b506100b2600480360360208110156100d757600080fd5b50356001600160a01b0316610686565b3480156100f357600080fd5b506100b2600480360361010081101561010b57600080fd5b6001600160a01b0382358116926020810135926040820135909216916060820135919081019060a081016080820135600160201b81111561014b57600080fd5b82018360208201111561015d57600080fd5b803590602001918460208302840111600160201b8311171561017e57600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295949360208101935035915050600160201b8111156101cd57600080fd5b8201836020820111156101df57600080fd5b803590602001918460208302840111600160201b8311171561020057600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295949360208101935035915050600160201b81111561024f57600080fd5b82018360208201111561026157600080fd5b803590602001918460208302840111600160201b8311171561028257600080fd5b919080806020026020016040519081016040528093929190818152602001838360200280828437600092019190915250929550505090356001600160a01b031691506106df9050565b3480156102d757600080fd5b506100b2600480360360e08110156102ee57600080fd5b6001600160a01b0382358116926020810135926040820135909216916060820135919081019060a081016080820135600160201b81111561032e57600080fd5b82018360208201111561034057600080fd5b803590602001918460208302840111600160201b8311171561036157600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295949360208101935035915050600160201b8111156103b057600080fd5b8201836020820111156103c257600080fd5b803590602001918460208302840111600160201b831117156103e357600080fd5b9190808060200260200160405190810160405280939291908181526020018383602002808284376000920191909152509295949360208101935035915050600160201b81111561043257600080fd5b82018360208201111561044457600080fd5b803590602001918460208302840111600160201b8311171561046557600080fd5b91908080602002602001604051908101604052809392919081815260200183836020028082843760009201919091525092955061089b945050505050565b60015460408051600160e21b6327c6855b0281526001600160a01b03848116600483015291519190921691639f1a156c916024808301926020929190829003018186803b1580156104f357600080fd5b505afa158015610507573d6000803e3d6000fd5b505050506040513d602081101561051d57600080fd5b505161052857600080fd5b6001600160a01b03811661057e57600080546040516001600160a01b036101009092049190911691303180156108fc02929091818181858888f19350505050158015610578573d6000803e3d6000fd5b50610683565b60005460408051600160e01b6370a08231028152306004820152905183926001600160a01b038085169363a9059cbb936101009093049091169184916370a08231916024808301926020929190829003018186803b1580156105df57600080fd5b505afa1580156105f3573d6000803e3d6000fd5b505050506040513d602081101561060957600080fd5b50516040805163ffffffff851660e01b81526001600160a01b03909316600484015260248301919091525160448083019260209291908290030181600087803b15801561065557600080fd5b505af1158015610669573d6000803e3d6000fd5b505050506040513d602081101561067f57600080fd5b5050505b50565b60005460ff161561069657600080fd5b6000805460018054610100610100600160a81b03199093166001600160a01b039586168402179283049094166001600160a01b031990941693909317835560ff19169091179055565b600054604080516101009092046001600160a01b03168252517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a49916020908290030190a1600154604051600160e11b6377514f1f0281526001600160a01b038a811660048301908152602483018b905289821660448401526064830189905284821660e48401526101006084840190815288516101048501528851929094169363eea29e3e938d938d938d938d938d938d938d938d939192909160a481019160c482019161012401906020808b01910280838360005b838110156107cd5781810151838201526020016107b5565b50505050905001848103835287818151815260200191508051906020019060200280838360005b8381101561080c5781810151838201526020016107f4565b50505050905001848103825286818151815260200191508051906020019060200280838360005b8381101561084b578181015183820152602001610833565b505050509050019b505050505050505050505050600060405180830381600087803b15801561087957600080fd5b505af115801561088d573d6000803e3d6000fd5b505050505050505050505050565b600054604080516101009092046001600160a01b03168252517fa0786e1009edc9cbf8898c0299c4518c0d18ec943fa88b2af645b4dd024d7a49916020908290030190a1600160009054906101000a90046001600160a01b03166001600160a01b031663f619ef13888888888888886040518863ffffffff1660e01b815260040180886001600160a01b03166001600160a01b03168152602001878152602001866001600160a01b03166001600160a01b03168152602001858152602001806020018060200180602001848103845287818151815260200191508051906020019060200280838360005b8381101561099d578181015183820152602001610985565b50505050905001848103835286818151815260200191508051906020019060200280838360005b838110156109dc5781810151838201526020016109c4565b50505050905001848103825285818151815260200191508051906020019060200280838360005b83811015610a1b578181015183820152602001610a03565b505050509050019a5050505050505050505050600060405180830381600087803b158015610a4857600080fd5b505af1158015610a5c573d6000803e3d6000fd5b505050505050505050505056fea165627a7a723058207a84f402c18866269b4d96afff802a1c19d2c19c9769bce8e47d0eb812036ff90029";

    public static final String FUNC_SENDTOMASTER = "sendToMaster";

    public static final String FUNC_INITIALIZE = "initialize";

    public static final String FUNC_WITHDRAW = "withdraw";

    public static final String FUNC_MINTTOKENSBYPEERS = "mintTokensByPeers";

    public static final Event ADDRESSEVENT_EVENT = new Event("AddressEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
    ;

    public static final Event STRINGEVENT_EVENT = new Event("StringEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
    ;

    public static final Event BYTESEVENT_EVENT = new Event("BytesEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
    ;

    public static final Event NUMBEREVENT_EVENT = new Event("NumberEvent", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected Relay(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected Relay(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected Relay(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected Relay(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public RemoteCall<TransactionReceipt> sendToMaster(String tokenAddress) {
        final Function function = new Function(
                FUNC_SENDTOMASTER, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> initialize(String master) {
        final Function function = new Function(
                FUNC_INITIALIZE, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> withdraw(String tokenAddress, BigInteger amount, String to, byte[] tx_hash, List<BigInteger> v, List<byte[]> r, List<byte[]> s, String from) {
        final Function function = new Function(
                FUNC_WITHDRAW, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(to), 
                new org.web3j.abi.datatypes.generated.Bytes32(tx_hash), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.datatypes.generated.Uint8.class,
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.Address(from)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> mintTokensByPeers(String tokenAddress, BigInteger amount, String beneficiary, byte[] txHash, List<BigInteger> v, List<byte[]> r, List<byte[]> s) {
        final Function function = new Function(
                FUNC_MINTTOKENSBYPEERS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(tokenAddress), 
                new org.web3j.abi.datatypes.generated.Uint256(amount), 
                new org.web3j.abi.datatypes.Address(beneficiary), 
                new org.web3j.abi.datatypes.generated.Bytes32(txHash), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Uint8>(
                        org.web3j.abi.datatypes.generated.Uint8.class,
                        org.web3j.abi.Utils.typeMap(v, org.web3j.abi.datatypes.generated.Uint8.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(r, org.web3j.abi.datatypes.generated.Bytes32.class)), 
                new org.web3j.abi.datatypes.DynamicArray<org.web3j.abi.datatypes.generated.Bytes32>(
                        org.web3j.abi.datatypes.generated.Bytes32.class,
                        org.web3j.abi.Utils.typeMap(s, org.web3j.abi.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public List<AddressEventEventResponse> getAddressEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(ADDRESSEVENT_EVENT, transactionReceipt);
        ArrayList<AddressEventEventResponse> responses = new ArrayList<AddressEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            AddressEventEventResponse typedResponse = new AddressEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<AddressEventEventResponse> addressEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, AddressEventEventResponse>() {
            @Override
            public AddressEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(ADDRESSEVENT_EVENT, log);
                AddressEventEventResponse typedResponse = new AddressEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<AddressEventEventResponse> addressEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(ADDRESSEVENT_EVENT));
        return addressEventEventFlowable(filter);
    }

    public List<StringEventEventResponse> getStringEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(STRINGEVENT_EVENT, transactionReceipt);
        ArrayList<StringEventEventResponse> responses = new ArrayList<StringEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            StringEventEventResponse typedResponse = new StringEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<StringEventEventResponse> stringEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, StringEventEventResponse>() {
            @Override
            public StringEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(STRINGEVENT_EVENT, log);
                StringEventEventResponse typedResponse = new StringEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (String) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<StringEventEventResponse> stringEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(STRINGEVENT_EVENT));
        return stringEventEventFlowable(filter);
    }

    public List<BytesEventEventResponse> getBytesEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(BYTESEVENT_EVENT, transactionReceipt);
        ArrayList<BytesEventEventResponse> responses = new ArrayList<BytesEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            BytesEventEventResponse typedResponse = new BytesEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<BytesEventEventResponse> bytesEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, BytesEventEventResponse>() {
            @Override
            public BytesEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(BYTESEVENT_EVENT, log);
                BytesEventEventResponse typedResponse = new BytesEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (byte[]) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<BytesEventEventResponse> bytesEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(BYTESEVENT_EVENT));
        return bytesEventEventFlowable(filter);
    }

    public List<NumberEventEventResponse> getNumberEventEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(NUMBEREVENT_EVENT, transactionReceipt);
        ArrayList<NumberEventEventResponse> responses = new ArrayList<NumberEventEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            NumberEventEventResponse typedResponse = new NumberEventEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<NumberEventEventResponse> numberEventEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, NumberEventEventResponse>() {
            @Override
            public NumberEventEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NUMBEREVENT_EVENT, log);
                NumberEventEventResponse typedResponse = new NumberEventEventResponse();
                typedResponse.log = log;
                typedResponse.input = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<NumberEventEventResponse> numberEventEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NUMBEREVENT_EVENT));
        return numberEventEventFlowable(filter);
    }

    @Deprecated
    public static Relay load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static Relay load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new Relay(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static Relay load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new Relay(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static Relay load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new Relay(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<Relay> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Relay> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<Relay> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String master) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(master)));
        return deployRemoteCall(Relay.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public static class AddressEventEventResponse {
        public Log log;

        public String input;
    }

    public static class StringEventEventResponse {
        public Log log;

        public String input;
    }

    public static class BytesEventEventResponse {
        public Log log;

        public byte[] input;
    }

    public static class NumberEventEventResponse {
        public Log log;

        public BigInteger input;
    }
}
