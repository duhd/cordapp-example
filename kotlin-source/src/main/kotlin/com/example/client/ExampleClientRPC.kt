package com.example.client

import com.example.flow.ExampleFlow
import com.example.state.IOUState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import kotlin.system.measureTimeMillis
import net.corda.core.identity.Party
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.OpaqueBytes
import net.corda.finance.USD
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.contracts.getCashBalance
import net.corda.finance.flows.CashIssueFlow
import net.corda.finance.flows.CashPaymentFlow
import java.util.concurrent.Executors


/**
 *  Demonstration of using the CordaRPCClient to connect to a Corda Node and
 *  steam some State data from the node.
 **/

fun main(args: Array<String>) {
    ExampleClientRPC().main(args)
}

private class ExampleClientRPC {
    companion object {
        val logger: Logger = loggerFor<ExampleClientRPC>()
        private fun logState(state: StateAndRef<IOUState>) = logger.info("{}", state.state.data)
    }

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ExampleClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse(args[0])
        val client = CordaRPCClient(nodeAddress)
        val conn = client.start("corda", "not_blockchain")
        val proxy: CordaRPCOps = conn.proxy

        val executor = Executors.newFixedThreadPool(1)

        val counterPartyName = CordaX500Name("BankA", "Hanoi", "VN")
        val otherParty = proxy.wellKnownPartyFromX500Name(counterPartyName)
        val notary = proxy.notaryIdentities().first()

        println("Sum Total: ${proxy.getCashBalance(USD)}")

        val forLoopMillisElapsed2 = measureTimeMillis {
            for (i in 0..999) {

                    if (otherParty != null) {
                        generateTransactions(proxy, otherParty, i)
                        //cashIssue(proxy,notary,i)
                    }

               }

        }
        println("forLoopMillisElapsed: $forLoopMillisElapsed2")
        println("Sum Total: ${proxy.getCashBalance(USD)}")
        println("Finished all threads")
        conn.notifyServerAndClose()
    }

    fun generateTransactions(proxy: CordaRPCOps, otherParty: Party, i: Int) {
        //println("$i..." + proxy.startFlow(::CashPaymentFlow, Amount(10, USD), otherParty).returnValue.getOrThrow().toString())
        println("$i..." + proxy.startFlow(::CashPaymentFlow, Amount(10, USD), otherParty).id)
    }

    fun cashIssue(proxy: CordaRPCOps, notary: Party, i: Int) {
        val issueRef = OpaqueBytes.of(0)
        println("$i..." + proxy.startFlow(::CashIssueFlow, Amount(100, USD), issueRef, notary).returnValue.getOrThrow().toString())
    }

}
