/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.contract;

import io.reactivex.Flowable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.2.0.
 */
public class BasicCoinManager extends Contract {
    public static final String FUNC_COUNTBYOWNER = "countByOwner";
    public static final String FUNC_COUNT = "count";
    public static final String FUNC_SETOWNER = "setOwner";
    public static final String FUNC_BASE = "base";
    public static final String FUNC_OWNER = "owner";
    public static final String FUNC_GET = "get";
    public static final String FUNC_DRAIN = "drain";
    public static final String FUNC_GETBYOWNER = "getByOwner";
    public static final Event CREATED_EVENT = new Event("Created",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));
    public static final Event NEWOWNER_EVENT = new Event("NewOwner",
            Arrays.<TypeReference<?>>asList(new TypeReference<Address>(true) {
            }, new TypeReference<Address>(true) {
            }));
    ;
    private static final String BINARY = "6080604052600080546001600160a01b0319163317905534801561002257600080fd5b50610f50806100326000396000f3fe6080604052600436106100865760003560e01c80638da5cb5b116100595780638da5cb5b1461012f5780639507d39a146101605780639890220b146101b5578063acfdfd1c146101ca578063c00ca3831461031d57610086565b8063061ea8cc1461008b57806306661abd146100d057806313af4035146100e55780635001f3b51461011a575b600080fd5b34801561009757600080fd5b506100be600480360360208110156100ae57600080fd5b50356001600160a01b0316610356565b60408051918252519081900360200190f35b3480156100dc57600080fd5b506100be610371565b3480156100f157600080fd5b506101186004803603602081101561010857600080fd5b50356001600160a01b0316610378565b005b34801561012657600080fd5b506100be6103ea565b34801561013b57600080fd5b506101446103f1565b604080516001600160a01b039092168252519081900360200190f35b34801561016c57600080fd5b5061018a6004803603602081101561018357600080fd5b5035610400565b604080516001600160a01b039485168152928416602084015292168183015290519081900360600190f35b3480156101c157600080fd5b5061011861046e565b610309600480360360808110156101e057600080fd5b8135919081019060408101602082013564010000000081111561020257600080fd5b82018360208201111561021457600080fd5b8035906020019184600183028401116401000000008311171561023657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929594936020810193503591505064010000000081111561028957600080fd5b82018360208201111561029b57600080fd5b803590602001918460018302840111640100000000831117156102bd57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550505090356001600160a01b031691506104ac9050565b604080519115158252519081900360200190f35b34801561032957600080fd5b5061018a6004803603604081101561034057600080fd5b506001600160a01b038135169060200135610869565b6001600160a01b031660009081526002602052604090205490565b6001545b90565b6000546001600160a01b0316331461038f57600080fd5b600080546040516001600160a01b03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b620f424081565b6000546001600160a01b031681565b600080600061040d6108b3565b6001858154811061041a57fe5b600091825260209182902060408051606081018252600390930290910180546001600160a01b0390811680855260018301548216958501869052600290920154169290910182905297919650945092505050565b6000546001600160a01b0316331461048557600080fd5b6040513390303180156108fc02916000818181858888f193505050506104aa57600080fd5b565b600080829050600086336040516104c2906108d3565b9182526001600160a01b03166020820152604080519182900301906000f0801580156104f2573d6000803e3d6000fd5b509050600061050033610356565b90506000836001600160a01b031663ddca3f436040518163ffffffff1660e01b815260040160206040518083038186803b15801561053d57600080fd5b505afa158015610551573d6000803e3d6000fd5b505050506040513d602081101561056757600080fd5b5051336000908152600260205260409020909150600183019061058a90826108e0565b506001543360009081526002602052604090208054849081106105a957fe5b906000526020600020018190555060016040518060600160405280856001600160a01b03168152602001336001600160a01b03168152602001866001600160a01b03168152509080600181540180825580915050906001820390600052602060002090600302016000909192909190915060008201518160000160006101000a8154816001600160a01b0302191690836001600160a01b0316021790555060208201518160010160006101000a8154816001600160a01b0302191690836001600160a01b0316021790555060408201518160020160006101000a8154816001600160a01b0302191690836001600160a01b03160217905550505050836001600160a01b0316637b1a547c82858b620f42408c336040518763ffffffff1660e01b815260040180866001600160a01b03166001600160a01b031681526020018060200185815260200180602001846001600160a01b03166001600160a01b03168152602001838103835287818151815260200191508051906020019080838360005b8381101561074257818101518382015260200161072a565b50505050905090810190601f16801561076f5780820380516001836020036101000a031916815260200191505b50838103825285518152855160209182019187019080838360005b838110156107a257818101518382015260200161078a565b50505050905090810190601f1680156107cf5780820380516001836020036101000a031916815260200191505b509750505050505050506020604051808303818588803b1580156107f257600080fd5b505af1158015610806573d6000803e3d6000fd5b50505050506040513d602081101561081d57600080fd5b50506040516001600160a01b03808516919086169033907f454b0172f64812df0cd504c2bd7df7aab8ff328a54d946b4bd0fa7c527adf9cc90600090a450600198975050505050505050565b6001600160a01b03821660009081526002602052604081208054829182916108a691908690811061089657fe5b9060005260206000200154610400565b9250925092509250925092565b604080516060810182526000808252602082018190529181019190915290565b6105f48061092883390190565b81548183558181111561090457600083815260209020610904918101908301610909565b505050565b61037591905b80821115610923576000815560010161090f565b509056fe60806040526000805460ff60a01b196001600160a01b0319909116331716905534801561002b57600080fd5b506040516105f43803806105f48339818101604052604081101561004e57600080fd5b508051602090910151818061006257600080fd5b506001829055600080546001600160a01b039092166001600160a01b03199092168217815590815260026020526040902055610551806100a36000396000f3fe608060405234801561001057600080fd5b506004361061009e5760003560e01c806350f9b6cd1161006657806350f9b6cd1461016357806370a082311461016b5780638da5cb5b14610191578063a9059cbb146101b5578063dd62ed3e146101e15761009e565b8063095ea7b3146100a357806313af4035146100e357806318160ddd1461010b57806323b872dd146101255780635001f3b51461015b575b600080fd5b6100cf600480360360408110156100b957600080fd5b506001600160a01b03813516906020013561020f565b604080519115158252519081900360200190f35b610109600480360360208110156100f957600080fd5b50356001600160a01b0316610289565b005b6101136102fb565b60408051918252519081900360200190f35b6100cf6004803603606081101561013b57600080fd5b506001600160a01b03813581169160208101359091169060400135610301565b610113610414565b6100cf61041b565b6101136004803603602081101561018157600080fd5b50356001600160a01b031661042b565b610199610446565b604080516001600160a01b039092168252519081900360200190f35b6100cf600480360360408110156101cb57600080fd5b506001600160a01b038135169060200135610455565b610113600480360360408110156101f757600080fd5b506001600160a01b03813581169160200135166104ed565b6040805182815290516000916001600160a01b0385169133917f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925919081900360200190a3503360009081526002602090815260408083206001600160a01b0386168452600190810190925290912080548301905592915050565b6000546001600160a01b031633146102a057600080fd5b600080546040516001600160a01b03808516939216917f70aea8d848e8a90fb7661b227dc522eb6395c3dac71b63cb59edd5c9899b236491a3600080546001600160a01b0319166001600160a01b0392909216919091179055565b60015481565b6001600160a01b0383166000908152600260205260408120548490839081111561032a57600080fd5b6001600160a01b038616600090815260026020908152604080832033808552600190910190925290912054879190869081111561036657600080fd5b6000805460ff60a01b1916600160a01b1790556040805188815290516001600160a01b03808b1692908c16917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a36001600160a01b03808a1660009081526002602081815260408084203385526001808201845282862080548f900390559390925281548c9003909155928b168252919020805489019055955050505050509392505050565b620f424081565b600054600160a01b900460ff1681565b6001600160a01b031660009081526002602052604090205490565b6000546001600160a01b031681565b33600081815260026020526040812054909190839081111561047657600080fd5b6040805185815290516001600160a01b0387169133917fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef9181900360200190a333600090815260026020526040808220805487900390556001600160a01b0387168252902080548501905560019250505092915050565b6001600160a01b039182166000908152600260209081526040808320939094168252600190920190915220549056fea265627a7a72305820a41e42d33b87bb4d9c56e738941de603f2e9b544e734c68725bc4e5b1b88e65664736f6c63430005090032a265627a7a72305820d2da4736ae3afdd1d58ab45c30a7a683325da9241594f8f7ada5d4997ae1935b64736f6c63430005090032";
    ;

    @Deprecated
    protected BasicCoinManager(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected BasicCoinManager(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected BasicCoinManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected BasicCoinManager(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static BasicCoinManager load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static BasicCoinManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new BasicCoinManager(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new BasicCoinManager(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static BasicCoinManager load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new BasicCoinManager(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(BasicCoinManager.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(BasicCoinManager.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<BasicCoinManager> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(BasicCoinManager.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
    }

    public RemoteCall<BigInteger> countByOwner(String _owner) {
        final Function function = new Function(FUNC_COUNTBYOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<BigInteger> count() {
        final Function function = new Function(FUNC_COUNT,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<TransactionReceipt> setOwner(String _new) {
        final Function function = new Function(
                FUNC_SETOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_new)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<BigInteger> base() {
        final Function function = new Function(FUNC_BASE,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {
                }));
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }

    public RemoteCall<String> owner() {
        final Function function = new Function(FUNC_OWNER,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<Tuple3<String, String, String>> get(BigInteger _index) {
        final Function function = new Function(FUNC_GET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteCall<Tuple3<String, String, String>>(
                new Callable<Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, String>(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (String) results.get(2).getValue());
                    }
                });
    }

    public RemoteCall<TransactionReceipt> drain() {
        final Function function = new Function(
                FUNC_DRAIN,
                Arrays.<Type>asList(),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<TransactionReceipt> deploy(BigInteger _totalSupply, String _tla, String _name, String _tokenreg, BigInteger weiValue) {
        final Function function = new Function(
                FUNC_DEPLOY,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(_totalSupply),
                        new org.web3j.abi.datatypes.Utf8String(_tla),
                        new org.web3j.abi.datatypes.Utf8String(_name),
                        new org.web3j.abi.datatypes.Address(_tokenreg)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function, weiValue);
    }

    public RemoteCall<Tuple3<String, String, String>> getByOwner(String _owner, BigInteger _index) {
        final Function function = new Function(FUNC_GETBYOWNER,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Address(_owner),
                        new org.web3j.abi.datatypes.generated.Uint256(_index)),
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }, new TypeReference<Address>() {
                }));
        return new RemoteCall<Tuple3<String, String, String>>(
                new Callable<Tuple3<String, String, String>>() {
                    @Override
                    public Tuple3<String, String, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, String, String>(
                                (String) results.get(0).getValue(),
                                (String) results.get(1).getValue(),
                                (String) results.get(2).getValue());
                    }
                });
    }

    public List<CreatedEventResponse> getCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CREATED_EVENT, transactionReceipt);
        ArrayList<CreatedEventResponse> responses = new ArrayList<CreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CreatedEventResponse typedResponse = new CreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.tokenreg = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.coin = (String) eventValues.getIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<CreatedEventResponse> createdEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, CreatedEventResponse>() {
            @Override
            public CreatedEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(CREATED_EVENT, log);
                CreatedEventResponse typedResponse = new CreatedEventResponse();
                typedResponse.log = log;
                typedResponse.owner = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.tokenreg = (String) eventValues.getIndexedValues().get(1).getValue();
                typedResponse.coin = (String) eventValues.getIndexedValues().get(2).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<CreatedEventResponse> createdEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(CREATED_EVENT));
        return createdEventFlowable(filter);
    }

    public List<NewOwnerEventResponse> getNewOwnerEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(NEWOWNER_EVENT, transactionReceipt);
        ArrayList<NewOwnerEventResponse> responses = new ArrayList<NewOwnerEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            NewOwnerEventResponse typedResponse = new NewOwnerEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.old = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.current = (String) eventValues.getIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public Flowable<NewOwnerEventResponse> newOwnerEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(new io.reactivex.functions.Function<Log, NewOwnerEventResponse>() {
            @Override
            public NewOwnerEventResponse apply(Log log) {
                Contract.EventValuesWithLog eventValues = extractEventParametersWithLog(NEWOWNER_EVENT, log);
                NewOwnerEventResponse typedResponse = new NewOwnerEventResponse();
                typedResponse.log = log;
                typedResponse.old = (String) eventValues.getIndexedValues().get(0).getValue();
                typedResponse.current = (String) eventValues.getIndexedValues().get(1).getValue();
                return typedResponse;
            }
        });
    }

    public Flowable<NewOwnerEventResponse> newOwnerEventFlowable(DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(NEWOWNER_EVENT));
        return newOwnerEventFlowable(filter);
    }

    public static class CreatedEventResponse {
        public Log log;

        public String owner;

        public String tokenreg;

        public String coin;
    }

    public static class NewOwnerEventResponse {
        public Log log;

        public String old;

        public String current;
    }
}
