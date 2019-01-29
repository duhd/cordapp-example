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
        val proxy = mutableListOf<CordaRPCOps>()
        val counterPartyName = CordaX500Name("BankB", "Hanoi", "VN")


        //So luong lenh
        val txCount = 999

        //So luong RPC connections
        val rpc = 63
        // Khoi tao ket noi RPC

        for (i in 0 .. rpc) {
            proxy.add(i, client.start("corda", "not_blockchain").proxy)
            println("RPC Connected...$i")
        }

        val executor = Executors.newFixedThreadPool(128)
        val otherParty = proxy.first().wellKnownPartyFromX500Name(counterPartyName)

        //Getbalance
        val vault = proxy.first().vaultQueryBy<Cash.State>().states
        var ownedQuantity = vault.fold(0L) { sum, state ->
            sum + state.state.data.amount.quantity
        }
        println("Sum Total: $ownedQuantity")


        val forLoopMillisElapsed = measureTimeMillis {
            var p = 0
            for (i in 0 .. 999) {
                //val notary = proxy.notaryIdentities().first()
                val worker = Runnable {
                    if (otherParty != null) {
                        generateTransactions(proxy[p], otherParty, i)
                    }
                }
                executor.execute(worker)
                if (p < rpc) p.plus(1) else p = 0
            }
            executor.shutdown()
            while (!executor.isTerminated) {
        }
        }
        println("forLoopMillisElapsed: $forLoopMillisElapsed")
        println("Finished all threads")
    }

    fun generateTransactions(proxy: CordaRPCOps, otherParty: Party, i: Int) {
        //println("$i..." + proxy.startFlow(ExampleFlow::Initiator, 99, otherParty).returnValue.getOrThrow().toString())
        //proxy.startFlow(ExampleFlow::Initiator, 99, otherParty)
        println("$i..." + proxy.startFlow(::CashPaymentFlow, Amount(1, USD), otherParty).returnValue.getOrThrow().toString())
    }


}
