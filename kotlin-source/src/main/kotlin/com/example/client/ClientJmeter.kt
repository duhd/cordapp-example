package com.example.client

import org.apache.jmeter.config.Arguments
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext
import org.apache.jmeter.samplers.SampleResult
import org.slf4j.LoggerFactory
import java.io.Serializable


class ClientJmeter : AbstractJavaSamplerClient(), Serializable {
    override fun getDefaultParameters(): Arguments {
        val defaultParameters = Arguments()
        defaultParameters.addArgument(NODE_TAG, "51.77.128.44:10003")
        defaultParameters.addArgument(USER_TAG, "corda")
        defaultParameters.addArgument(PASS_TAG, "not_blockchain")
        defaultParameters.addArgument(RECEIVER_TAG, "BankB")
        defaultParameters.addArgument(AMOUNT_TAG, "10")
        defaultParameters.addArgument(METHOD_TAG, "pay")
        return defaultParameters
    }

    override fun setupTest(context: JavaSamplerContext) {
        val node = context.getParameter(NODE_TAG)
        val userRPC = context.getParameter(USER_TAG)
        val passRPC = context.getParameter(PASS_TAG)
        try {
            client.connect(node, userRPC, passRPC)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Error Connect RPC:", e)
        }
    }

    override fun runTest(context: JavaSamplerContext): SampleResult {
        val method = context.getParameter(METHOD_TAG)
        val receiver = context.getParameter(RECEIVER_TAG)
        val amount = context.getParameter(AMOUNT_TAG).toLong()
        val sampleResult = SampleResult()
        sampleResult.sampleStart()
        try {
            if (method.equals("pay")) {
                sampleResult.responseMessage = client.clientPay(receiver, amount)
            }else{
                sampleResult.responseMessage = client.clientIssue(amount)
            }
            sampleResult.sampleEnd()
            sampleResult.isSuccessful = java.lang.Boolean.TRUE
            sampleResult.setResponseCodeOK()
        } catch (e: Exception) {
            LOGGER.error("Request $method was not successfully processed", e)
            sampleResult.sampleEnd()
            sampleResult.responseMessage = e.message
            sampleResult.isSuccessful = java.lang.Boolean.FALSE
        }

        return sampleResult
    }

    override fun teardownTest(context: JavaSamplerContext) {
        client.disconnect()
    }

    companion object {
        private val client = ClientRPC()
        private val RECEIVER_TAG = "receiver"
        private val METHOD_TAG = "method"
        private val AMOUNT_TAG = "amount"
        private val NODE_TAG = "node"
        private val USER_TAG = "user"
        private val PASS_TAG = "pass"
        private val LOGGER = LoggerFactory.getLogger(ClientJmeter::class.java)
    }
}