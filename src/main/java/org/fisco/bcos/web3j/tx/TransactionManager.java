package org.fisco.bcos.web3j.tx;

import org.fisco.bcos.web3j.crypto.Credentials;
import org.fisco.bcos.web3j.protocol.Web3j;
import org.fisco.bcos.web3j.protocol.core.methods.response.EthSendTransaction;
import org.fisco.bcos.web3j.protocol.core.methods.response.TransactionReceipt;
import org.fisco.bcos.web3j.protocol.exceptions.TransactionException;
import org.fisco.bcos.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.fisco.bcos.web3j.tx.response.TransactionReceiptProcessor;

import java.io.IOException;
import java.math.BigInteger;

import static org.fisco.bcos.web3j.protocol.core.JsonRpc2_0Web3j.BLOCK_TIME;

/**
 * Transaction manager abstraction for executing transactions with Ethereum client via
 * various mechanisms.
 */
public abstract class TransactionManager {

    public static final int DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH = 40;
    public static final long DEFAULT_POLLING_FREQUENCY = BLOCK_TIME;  //15 * 100

    private final TransactionReceiptProcessor transactionReceiptProcessor;
    final Credentials credentials;

    protected TransactionManager(
            TransactionReceiptProcessor transactionReceiptProcessor, Credentials credentials) {
        this.transactionReceiptProcessor = transactionReceiptProcessor;
        this.credentials = credentials;
    }

    protected TransactionManager(Web3j web3j, Credentials credentials) {
        this(new PollingTransactionReceiptProcessor(
                        web3j, DEFAULT_POLLING_FREQUENCY, DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH),
                credentials);
    }

    protected TransactionManager(
            Web3j web3j, int attempts, long sleepDuration, Credentials credentials) {
        this(new PollingTransactionReceiptProcessor(web3j, sleepDuration, attempts), credentials);
    }

    protected TransactionReceipt executeTransaction(
            BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value)
            throws IOException, TransactionException {

        EthSendTransaction ethSendTransaction = sendTransaction(
                gasPrice, gasLimit, to, data, value);
        return processResponse(ethSendTransaction);
    }

    public abstract EthSendTransaction sendTransaction(
            BigInteger gasPrice, BigInteger gasLimit, String to,
            String data, BigInteger value)
            throws IOException;

    public String getFromAddress() {
        return credentials.getAddress();
    }

    private TransactionReceipt processResponse(EthSendTransaction transactionResponse)
            throws IOException, TransactionException {
        if (transactionResponse.hasError()) {
            throw new RuntimeException("Error processing transaction request: "
                    + transactionResponse.getError().getMessage());
        }

        String transactionHash = transactionResponse.getTransactionHash();

        return transactionReceiptProcessor.waitForTransactionReceipt(transactionHash);
    }

}
