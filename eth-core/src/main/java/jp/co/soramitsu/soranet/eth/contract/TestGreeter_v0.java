/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.contract;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.2.0.
 */
public class TestGreeter_v0 extends Contract {
    public static final String FUNC_SET = "set";
    public static final String FUNC_GREET = "greet";
    public static final String FUNC_INITIALIZE = "initialize";
    private static final String BINARY = "608060405234801561001057600080fd5b506040516105363803806105368339818101604052602081101561003357600080fd5b81019080805164010000000081111561004b57600080fd5b8201602081018481111561005e57600080fd5b815164010000000081118282018710171561007857600080fd5b509093506100939250839150506001600160e01b0361009916565b50610168565b60005460ff16156100a957600080fd5b80516100bc9060019060208401906100cd565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061010e57805160ff191683800117855561013b565b8280016001018555821561013b579182015b8281111561013b578251825591602001919060010190610120565b5061014792915061014b565b5090565b61016591905b808211156101475760008155600101610151565b90565b6103bf806101776000396000f3fe608060405234801561001057600080fd5b50600436106100415760003560e01c80634ed3885e14610046578063cfae3217146100ee578063f62d18881461016b575b600080fd5b6100ec6004803603602081101561005c57600080fd5b81019060208101813564010000000081111561007757600080fd5b82018360208201111561008957600080fd5b803590602001918460018302840111640100000000831117156100ab57600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610211945050505050565b005b6100f6610228565b6040805160208082528351818301528351919283929083019185019080838360005b83811015610130578181015183820152602001610118565b50505050905090810190601f16801561015d5780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6100ec6004803603602081101561018157600080fd5b81019060208101813564010000000081111561019c57600080fd5b8201836020820111156101ae57600080fd5b803590602001918460018302840111640100000000831117156101d057600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295506102be945050505050565b80516102249060019060208401906102f2565b5050565b60018054604080516020601f600260001961010087891615020190951694909404938401819004810282018101909252828152606093909290918301828280156102b35780601f10610288576101008083540402835291602001916102b3565b820191906000526020600020905b81548152906001019060200180831161029657829003601f168201915b505050505090505b90565b60005460ff16156102ce57600080fd5b80516102e19060019060208401906102f2565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061033357805160ff1916838001178555610360565b82800160010185558215610360579182015b82811115610360578251825591602001919060010190610345565b5061036c929150610370565b5090565b6102bb91905b8082111561036c576000815560010161037656fea265627a7a723058209501e61c3d1f29408630a4f8ed519a53dd25a87be327bdd2d003cf04fd87539e64736f6c63430005090032";

    @Deprecated
    protected TestGreeter_v0(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TestGreeter_v0(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TestGreeter_v0(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TestGreeter_v0(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v0(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v0(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v0(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TestGreeter_v0 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v0(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, credentials, contractGasProvider, BINARY, encodedConstructor);
    }

    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, transactionManager, contractGasProvider, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, credentials, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v0> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String greeting) {
        String encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)));
        return deployRemoteCall(TestGreeter_v0.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, encodedConstructor);
    }

    public RemoteCall<TransactionReceipt> set(String greeting) {
        final Function function = new Function(
                FUNC_SET,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    public RemoteCall<String> greet() {
        final Function function = new Function(FUNC_GREET,
                Arrays.<Type>asList(),
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {
                }));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteCall<TransactionReceipt> initialize(String greeting) {
        final Function function = new Function(
                FUNC_INITIALIZE,
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(greeting)),
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }
}
