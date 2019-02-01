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
import java.util.concurrent.ExecutorService
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

    var iError = 0

    fun main(args: Array<String>) {
        require(args.size == 1) { "Usage: ExampleClientRPC <node address>" }
        val nodeAddress = NetworkHostAndPort.parse("51.77.128.44:10003")
        val client = CordaRPCClient(nodeAddress)
        val conn = client.start("corda", "not_blockchain")
        val proxy = mutableListOf<CordaRPCOps>()
        val rpcs = 127


        for (i in 0 until rpcs) {
            proxy.add(i, client.start("corda", "not_blockchain").proxy)
            println("RPC Connected...$i")
        }

        val executor: ExecutorService = Executors.newFixedThreadPool(128)

        val counterPartyName = CordaX500Name("BankB", "Hanoi", "VN")
        val otherParty = proxy.first().wellKnownPartyFromX500Name(counterPartyName)
        val notary = proxy.first().notaryIdentities().first()

        println("Sum Total: ${proxy.first().getCashBalance(USD)}")

        var p: Int = 0
        val forLoopMillisElapsed2 = measureTimeMillis {
            for (i in 0..9999) {
                println("PPPPPPPPPPPPP: $p")
                val worker = Runnable {
                    if (otherParty != null) {
                        //cashIssue(proxy[p], notary, i)
                        generateTransactions(proxy[p], otherParty, i, p)
                    }
                }
                executor.execute(worker)
                if (p < rpcs) p += 1 else p = 0
            }
            executor.shutdown()
            while (!executor.isTerminated) {
            }
        }
        println("forLoopMillisElapsed: $forLoopMillisElapsed2")
        println("ErrorTX: $iError")
        println("Sum Total: ${proxy.first().getCashBalance(USD)}")
        println("Finished all threads")
        conn.notifyServerAndClose()
    }

    fun generateTransactions(proxy: CordaRPCOps, otherParty: Party, i: Int, p: Int) {

        try {
            var tx = proxy.startFlow(::CashPaymentFlow, Amount(100, USD), otherParty).returnValue.getOrThrow()
            //var tx = proxy.startFlow(ExampleFlow::Initiator, 99, otherParty).returnValue.getOrThrow()


            //println("$i..." + proxy.startFlow(::CashPaymentFlow, Amount(10, USD), otherParty).id)
            //println("$i..." + proxy.startFlow(ExampleFlow::Initiator, 99, otherParty).returnValue.getOrThrow().toString())
            //proxy.startFlow(ExampleFlow::Initiator, 99, otherParty)
            println("$i-$p..." + tx.toString())
        } catch (exception: Exception) {
            iError += 1
            println("$i-$p...------------------------------------------------------------")
        }


    }

    fun cashIssue(proxy: CordaRPCOps, notary: Party, i: Int) {
        val issueRef = OpaqueBytes.of(0)
        println("$i..." + proxy.startFlow(::CashIssueFlow, Amount(100, USD), issueRef, notary).returnValue.getOrThrow().toString())
    }

}
