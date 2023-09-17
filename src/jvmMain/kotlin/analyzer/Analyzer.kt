package analyzer

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import convertShortChannelId
import estimateDemand
import kotlinx.coroutines.*
import model.ground_truth_simulator_outputs.ChannelOutput
import model.ground_truth_simulator_outputs.EdgeOutput
import model.ground_truth_simulator_outputs.NodeOutput
import model.ground_truth_simulator_outputs.PaymentOutput
import model.input_gossip_msg.ChannelAnnouncement
import model.input_gossip_msg.ChannelUpdate
import network.Channel
import network.Direction
import network.Network
import network.Node
import printDemand
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun genNetworkFromLNDOutputs(
    describeGraphJsonFile: File,
    channelAnnouncementCSVFile: File,
    channelUpdateCSVFile: File
): Network {
    val channels = mutableMapOf<String, Channel>()
    val nodes = mutableMapOf<String, Node>()
    val network = Network(channels, nodes)

    // load describeGraphJsonFile
    val json = ObjectMapper().readValue(
        describeGraphJsonFile,
        object : TypeReference<Map<String, List<Map<String, Any>>>>() {},
    )
    json["nodes"]?.forEach {
        val id = it["pub_key"] as String
        nodes[id] = Node(id, network)
    }
    json["edges"]?.forEach {
        val id = convertShortChannelId((it["channel_id"] as String).toLong())
        val capacity = (it["capacity"] as String).toLong()
        val node1 = nodes[it["node1_pub"]] ?: return@forEach
        val node2 = nodes[it["node2_pub"]] ?: return@forEach
        channels[id] = Channel(id, node1, node2, capacity, network)
    }

    // load channelUpdateCSVFile

    BufferedReader(FileReader(channelAnnouncementCSVFile)).use { br ->
        var line: String?

        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: return@use

            try {
                val channelAnnouncement = ChannelAnnouncement(
                    csvElements[0],
                    csvElements[1],
                    csvElements[2],
                    csvElements[3],
                    csvElements[4],
                    csvElements[5],
                    csvElements[6],
                    csvElements[7],
                    csvElements[8],
                    csvElements[9],
                    csvElements[10],
                )

                val node1 = nodes[channelAnnouncement.nodeId1] ?: Node(channelAnnouncement.nodeId1, network)
                val node2 = nodes[channelAnnouncement.nodeId2] ?: Node(channelAnnouncement.nodeId2, network)
                val channel = channels[channelAnnouncement.shortChannelId] ?: Channel(
                    channelAnnouncement.shortChannelId,
                    node1,
                    node2,
                    0L,
                    network
                )
                node1.channels.add(channel)
                node2.channels.add(channel)
                nodes[channelAnnouncement.nodeId1] = node1
                nodes[channelAnnouncement.nodeId2] = node2
                channels[channelAnnouncement.shortChannelId] = channel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BufferedReader(FileReader(channelUpdateCSVFile)).use { br ->
        var line: String?

        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: return@use

            try {
                val timestamp = try {
                    LocalDateTime.parse(
                        csvElements[3],
                        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                    ).toEpochSecond(ZoneOffset.UTC)
                } catch (e: Exception) {
                    csvElements[3].toLong()
                }

                val id = csvElements[2]

                val channelUpdate = ChannelUpdate(
                    csvElements[0],
                    csvElements[1],
                    id,
                    timestamp,
                    csvElements[4],
                    csvElements[5],
                    csvElements[6],
                    csvElements[7].toLong(),
                    csvElements[8].toLong(),
                    csvElements[9].toLong(),
                    csvElements[10].toLong(),
                )

                val channel = channels[channelUpdate.shortChannelId]
                channel?.addChannelUpdate(channelUpdate)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    estimateDemand(network)
    printDemand(network)

    return network
}

// load CLoTH outputs csv file
fun genGroundTruthNetworkFromSimulatorOutput(
    edgesOutputCSVFile: File,
    nodesOutputCSVFile: File,
    channelsOutputCSVFile: File,
    paymentsOutputCSVFile: File,
): Network {
    val channels = mutableMapOf<String, Channel>()
    val nodes = mutableMapOf<String, Node>()
    val network = Network(channels, nodes)

    val edges = mutableMapOf<String, EdgeOutput>()

    BufferedReader(FileReader(edgesOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: continue
            try {
                val edgeOutput = EdgeOutput(
                    csvElements[0],
                    csvElements[1],
                    csvElements[2],
                    csvElements[3],
                    csvElements[4],
                    csvElements[5],
                    csvElements[6],
                    csvElements[7],
                    csvElements[8],
                    csvElements[9],
                    csvElements[10],
                    csvElements[11],
                )

                edges[csvElements[0]] = edgeOutput
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BufferedReader(FileReader(nodesOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: continue
            try {
                val openEdges = mutableListOf<EdgeOutput>()
                csvElements[1].split("-").forEach {
                    openEdges.add(edges[it] ?: return@forEach)
                }

                val nodeId = csvElements[0]

                nodes[nodeId] = Node(nodeId, network)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BufferedReader(FileReader(channelsOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: continue
            try {
                val channelOutput = ChannelOutput(
                    csvElements[0],
                    csvElements[1],
                    csvElements[2],
                    csvElements[3],
                    csvElements[4],
                    csvElements[5],
                    csvElements[6],
                )

                val node1 = nodes[channelOutput.node1] ?: continue
                val node2 = nodes[channelOutput.node2] ?: continue

                val channel = Channel(channelOutput.id, node1, node2, channelOutput.capacity.toLong(), network)

                node1.channels.add(channel)
                node2.channels.add(channel)
                channels[channelOutput.id] = channel
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    BufferedReader(FileReader(paymentsOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            val csvElements = line?.split(",") ?: continue

            try {
                val route = mutableListOf<EdgeOutput>()
                csvElements[12].split("-").forEach {
                    route.add(edges[it] ?: return@forEach)
                }

                val paymentOutput = PaymentOutput(
                    csvElements[0],
                    csvElements[1],
                    csvElements[2],
                    csvElements[3],
                    csvElements[4],
                    csvElements[5],
                    csvElements[6],
                    csvElements[7],
                    csvElements[8],
                    csvElements[9],
                    csvElements[10],
                    csvElements[11],
                    route,
                    csvElements[13],
                )

                route.forEach { edge ->
                    val channel = channels[edge.channelId] ?: return@forEach
                    if (channel.node1.id == edge.fromNodeId && channel.node2.id == edge.toNodeId) {
                        // decrease capacity of edge
                        channels[edge.channelId]?.addChannelUpdate(
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
                                channel.edgeNode1ToNode2.capacity - paymentOutput.amount.toLong(),
                            )
                        )
                        // increase capacity of counter edge
                        channels[edge.channelId]?.addChannelUpdate(
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
                                channel.edgeNode2ToNode1.capacity + paymentOutput.amount.toLong(),
                            )
                        )
                    } else if (channel.node1.id == edge.toNodeId && channel.node2.id == edge.fromNodeId) {
                        // decrease capacity of edge
                        channels[edge.channelId]?.addChannelUpdate(
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
                                channel.edgeNode2ToNode1.capacity - paymentOutput.amount.toLong(),
                            )
                        )
                        // increase capacity of counter edge
                        channels[edge.channelId]?.addChannelUpdate(
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
                                channel.edgeNode1ToNode2.capacity + paymentOutput.amount.toLong(),
                            )
                        )
                    } else {
                        return@forEach
                    }
                }

                val senderNode = nodes[paymentOutput.senderId] ?: continue
                network.demand[senderNode] = (network.demand[senderNode] ?: 0L) + paymentOutput.amount.toLong()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    printDemand(network)

    return network
}