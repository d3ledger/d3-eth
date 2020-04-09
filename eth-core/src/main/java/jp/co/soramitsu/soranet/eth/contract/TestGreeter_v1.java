/*
 * Copyright Soramitsu Co., Ltd. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package jp.co.soramitsu.soranet.eth.contract;

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
public class TestGreeter_v1 extends Contract {
    public static final String FUNC_SET = "set";
    public static final String FUNC_GREET = "greet";
    public static final String FUNC_FAREWELL = "farewell";
    public static final String FUNC_INITIALIZE = "initialize";
    private static final String BINARY = "60c0604052600a60809081527f48692c20576f726c64210000000000000000000000000000000000000000000060a052610041816001600160e01b0361004716565b50610116565b60005460ff161561005757600080fd5b805161006a90600190602084019061007b565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f106100bc57805160ff19168380011785556100e9565b828001600101855582156100e9579182015b828111156100e95782518255916020019190600101906100ce565b506100f59291506100f9565b5090565b61011391905b808211156100f557600081556001016100ff565b90565b6103f5806101256000396000f3fe608060405234801561001057600080fd5b506004361061004c5760003560e01c80634ed3885e14610051578063cfae3217146100f9578063eca386af14610176578063f62d18881461017e575b600080fd5b6100f76004803603602081101561006757600080fd5b81019060208101813564010000000081111561008257600080fd5b82018360208201111561009457600080fd5b803590602001918460018302840111640100000000831117156100b657600080fd5b91908080601f016020809104026020016040519081016040528093929190818152602001838380828437600092019190915250929550610224945050505050565b005b61010161023b565b6040805160208082528351818301528351919283929083019185019080838360005b8381101561013b578181015183820152602001610123565b50505050905090810190601f1680156101685780820380516001836020036101000a031916815260200191505b509250505060405180910390f35b6101016102d1565b6100f76004803603602081101561019457600080fd5b8101906020810181356401000000008111156101af57600080fd5b8201836020820111156101c157600080fd5b803590602001918460018302840111640100000000831117156101e357600080fd5b91908080601f0160208091040260200160405190810160405280939291908181526020018383808284376000920191909152509295506102f4945050505050565b8051610237906001906020840190610328565b5050565b60018054604080516020601f600260001961010087891615020190951694909404938401819004810282018101909252828152606093909290918301828280156102c65780601f1061029b576101008083540402835291602001916102c6565b820191906000526020600020905b8154815290600101906020018083116102a957829003601f168201915b505050505090505b90565b604080518082019091526009815268476f6f64206279652160b81b602082015290565b60005460ff161561030457600080fd5b8051610317906001906020840190610328565b50506000805460ff19166001179055565b828054600181600116156101000203166002900490600052602060002090601f016020900481019282601f1061036957805160ff1916838001178555610396565b82800160010185558215610396579182015b8281111561039657825182559160200191906001019061037b565b506103a29291506103a6565b5090565b6102ce91905b808211156103a257600081556001016103ac56fea265627a7a723058207a9f0ec27af2dd42ba71d30d5928b14c4ad56cd16b902a0dffe7e349f9724f4464736f6c63430005090032";

    @Deprecated
    protected TestGreeter_v1(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TestGreeter_v1(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TestGreeter_v1(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TestGreeter_v1(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    @Deprecated
    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v1(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TestGreeter_v1(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v1(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TestGreeter_v1 load(String contractAddress, Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TestGreeter_v1(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, credentials, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, Credentials credentials, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, credentials, gasPrice, gasLimit, BINARY, "");
    }

    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, transactionManager, contractGasProvider, BINARY, "");
    }

    @Deprecated
    public static RemoteCall<TestGreeter_v1> deploy(Web3j web3j, TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TestGreeter_v1.class, web3j, transactionManager, gasPrice, gasLimit, BINARY, "");
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

    public RemoteCall<String> farewell() {
        final Function function = new Function(FUNC_FAREWELL,
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
