package analyzer

import channelsOutputAnalyzer
import edgesOutputAnalyzer
import model.ground_truth_simulator_outputs.EdgeOutput
import model.ground_truth_simulator_outputs.PaymentOutput
import model.input_gossip_msg.ChannelUpdate
import network.Direction
import network.Edge
import network.Network

class PaymentsOutputAnalyzer(private val groundTruthNetwork: Network) : CSVAnalyzer() {
    val payments = mutableMapOf<String, PaymentOutput>()

    override fun analyzeCSVLine(csvElements: List<String>) {
        val route = mutableListOf<EdgeOutput>()
        csvElements[1].split("-").forEach {
            route.add(edgesOutputAnalyzer.edges[it] ?: return@forEach)
        }

        val paymentOutput = PaymentOutput(
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            csvElements[0],
            route,
            csvElements[0],
        )

        payments[paymentOutput.id] = paymentOutput

        route.forEach { edge ->
            val channel = groundTruthNetwork.channels[edge.channelId] ?: return@forEach
            if (channel.node1.id == edge.fromNodeId && channel.node2.id == edge.toNodeId) {
                // decrease capacity of edge
                groundTruthNetwork.channels[edge.channelId]?.addChannelUpdate(
                    ChannelUpdate(
                        "",
                        "",
                        edge.channelId,
                        paymentOutput.startTime.toLong(),
                        "",
                        Direction.Node1ToNode2.toString(),
                        "",
                        0L,
                        0L,
                        0L,
                        (channel.edgeNode1ToNode2.capacity
                            ?: (channelsOutputAnalyzer.channels[channel.shortChannelId]?.capacity?.toLong()
                                ?: 0L)) - paymentOutput.amount.toLong(),
                    )
                )
                // increase capacity of counter edge
                groundTruthNetwork.channels[edge.channelId]?.addChannelUpdate(
                    ChannelUpdate(
                        "",
                        "",
                        edge.channelId,
                        paymentOutput.startTime.toLong(),
                        "",
                        Direction.Node2ToNode1.toString(),
                        "",
                        0L,
                        0L,
                        0L,
                        (channel.edgeNode2ToNode1.capacity
                            ?: (channelsOutputAnalyzer.channels[channel.shortChannelId]?.capacity?.toLong()
                                ?: 0L)) + paymentOutput.amount.toLong(),
                    )
                )
            } else if (channel.node1.id == edge.toNodeId && channel.node2.id == edge.fromNodeId) {

                // decrease capacity of edge
                groundTruthNetwork.channels[edge.channelId]?.addChannelUpdate(
                    ChannelUpdate(
                        "",
                        "",
                        edge.channelId,
                        paymentOutput.startTime.toLong(),
                        "",
                        Direction.Node2ToNode1.toString(),
                        "",
                        0L,
                        0L,
                        0L,
                        (channel.edgeNode2ToNode1.capacity
                            ?: (channelsOutputAnalyzer.channels[channel.shortChannelId]?.capacity?.toLong()
                                ?: 0L)) - paymentOutput.amount.toLong(),
                    )
                )
                // increase capacity of counter edge
                groundTruthNetwork.channels[edge.channelId]?.addChannelUpdate(
                    ChannelUpdate(
                        "",
                        "",
                        edge.channelId,
                        paymentOutput.startTime.toLong(),
                        "",
                        Direction.Node1ToNode2.toString(),
                        "",
                        0L,
                        0L,
                        0L,
                        (channel.edgeNode1ToNode2.capacity
                            ?: (channelsOutputAnalyzer.channels[channel.shortChannelId]?.capacity?.toLong()
                                ?: 0L)) + paymentOutput.amount.toLong(),
                    )
                )
            } else {
                return@forEach
            }
        }
    }

    override fun onAnalyzingFinished() {
    }
}