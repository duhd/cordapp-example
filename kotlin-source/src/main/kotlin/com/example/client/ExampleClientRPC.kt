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
import net.corda.core.messaging.vaultQueryBy
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



        logger.info(proxy.currentNodeTime().toString())



        //So luong lenh
        val txCount = 999

        //So luong RPC connections
        val rpc = 0

        val executor = Executors.newFixedThreadPool(256)
        val executor1 = Executors.newFixedThreadPool(500)
        val counterPartyName = CordaX500Name("BankA", "Hanoi", "VN")
        val otherParty = proxy.wellKnownPartyFromX500Name(counterPartyName)
        val notary = proxy.notaryIdentities().first()


//        val forLoopMillisElapsed1 = measureTimeMillis {
//            for (i in 0..999) {
//                val worker = Runnable {
//                    if (otherParty != null) {
//                        cashIssue(proxy, notary, i)
//                    }
//                }
//                executor1.execute(worker)
//            }
//            while (!executor1.isTerminated) {
//            }
//        }
//
//        println("forLoopMillisElapsed: $forLoopMillisElapsed1")
        println("Sum Total: ${proxy.getCashBalance(USD)}")


        val forLoopMillisElapsed2 = measureTimeMillis {
            for (i in 0..999) {
                val worker = Runnable {
                    if (otherParty != null) {
                        generateTransactions(proxy, otherParty, i)
                    }
                }
                executor.execute(worker)
               }
            executor.shutdown()
            while (!executor.isTerminated) {
            }
        }
        println("forLoopMillisElapsed: $forLoopMillisElapsed2")
        println("Finished all threads")
        conn.notifyServerAndClose()
    }

    fun generateTransactions(proxy: CordaRPCOps, otherParty: Party, i: Int) {
        //println("$i..." + proxy.startFlow(ExampleFlow::Initiator, 99, otherParty).returnValue.getOrThrow().toString())
        //proxy.startFlow(ExampleFlow::Initiator, 99, otherParty)
        println("$i..." + proxy.startFlow(::CashPaymentFlow, Amount(10, USD), otherParty).returnValue.getOrThrow().toString())
    }

    fun cashIssue(proxy: CordaRPCOps, notary: Party, i: Int) {
        val issueRef = OpaqueBytes.of(0)
        println("$i..." + proxy.startFlow(::CashIssueFlow, Amount(100, USD), issueRef, notary).returnValue.getOrThrow().toString())
    }

}
