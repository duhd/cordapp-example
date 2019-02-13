package com.example.client

import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.CordaRPCConnection
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.identity.Party
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.core.utilities.loggerFor
import net.corda.finance.USD
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow
import org.slf4j.Logger
import java.util.*

class ClientRPC {
    companion object {
        private var proxy: CordaRPCOps? = null
        private var conn: CordaRPCConnection? = null
        val logger: Logger = loggerFor<ClientRPC>()
    }

    fun connect(node: String, userRPC: String, passRPC: String) {
        val nodeAddress = NetworkHostAndPort.parse(node)
        val client = CordaRPCClient(nodeAddress)
        conn = client.start(userRPC, passRPC)
        proxy = conn!!.proxy
    }

    fun disconnect() {
        conn!!.notifyServerAndClose()
    }

    fun clientPay(receiver: String, amount: Long): String {
        val counterPartyName = CordaX500Name(receiver, "Hanoi", "VN")
        val otherParty = proxy!!.wellKnownPartyFromX500Name(counterPartyName)
        val random = Random()
        return cashPayment(proxy!!, otherParty!!, random.nextInt(9999).toLong())

    }

    fun clientIssueAndPay(receiver: String, amount: Long): String {
        val notary = proxy!!.notaryIdentities().first()
        val counterPartyName = CordaX500Name(receiver, "Hanoi", "VN")
        val otherParty = proxy!!.wellKnownPartyFromX500Name(counterPartyName)
        val random = Random()
        return cashIssueAndPayment(proxy!!, otherParty!!, notary, random.nextInt(1000).toLong())
    }

    fun clientIssue(amount: Long): String {
        val notary = proxy!!.notaryIdentities().first()
        val random = Random()
        val tx = cashIssue(proxy!!, notary, random.nextInt(10000).toLong())
        logger.debug(tx)
        return tx
    }

    fun cashPayment(proxy: CordaRPCOps, otherParty: Party, amount: Long): String {
        var tx = proxy.startFlow(::CashPaymentFlow, Amount(amount, USD), otherParty).returnValue.getOrThrow()
        return tx.toString()
    }

    fun cashIssueAndPayment(proxy: CordaRPCOps, otherParty: Party, notary: Party, amount: Long): String {
        val issueRef = OpaqueBytes.of(0)
        val request: CashIssueAndPaymentFlow.IssueAndPaymentRequest = CashIssueAndPaymentFlow.IssueAndPaymentRequest(Amount(amount, USD), issueRef, otherParty, notary, false)
        var tx = proxy.startFlow(::CashIssueAndPaymentFlow, request).returnValue.getOrThrow()
        return tx.toString()
    }

    fun cashIssue(proxy: CordaRPCOps, notary: Party, amount: Long): String {
        val issueRef = OpaqueBytes.of(0)
        val tx = proxy.startFlow(::CashIssueFlow, Amount(amount, USD), issueRef, notary).returnValue.getOrThrow()
        return tx.toString()
    }
}
