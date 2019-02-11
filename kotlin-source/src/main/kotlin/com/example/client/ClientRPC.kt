package com.example.client

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import net.corda.core.identity.Party
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.USD
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow


/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    ClientRPC().main(args)
}

private class ClientRPC {
    companion object {
        val logger: Logger = loggerFor<ClientRPC>()
    }
    fun main(args: Array<String>) {
        val nodeAddress = NetworkHostAndPort.parse("51.77.128.44:10003")
        val client = CordaRPCClient(nodeAddress)
        val conn = client.start("corda", "not_blockchain")

        val proxy: CordaRPCOps = conn.proxy
        val counterPartyName = CordaX500Name("BankB", "Hanoi", "VN")
        val otherParty = proxy.wellKnownPartyFromX500Name(counterPartyName)
        val notary = proxy.notaryIdentities().first()
        otherParty?.let { generateTransactions(proxy, it) }
        conn.notifyServerAndClose()
    }

    fun generateTransactions(proxy: CordaRPCOps, otherParty: Party) {
            var tx = proxy.startFlow(::CashPaymentFlow, Amount(100, USD), otherParty).returnValue.getOrThrow()
            println(tx.toString())
    }
    fun cashIssue(proxy: CordaRPCOps, notary: Party) {
        val issueRef = OpaqueBytes.of(0)
        println(proxy.startFlow(::CashIssueFlow, Amount(100, USD), issueRef, notary).returnValue.getOrThrow().toString())
    }

}
