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
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun genNetworkFromLNDOutputs(
    describeGraphJsonFile: File,
    channelAnnouncementCSVFile: File,
    channelUpdateCSVFile: File,
    onProgressChanged: (progress: Float, processTitle: String) -> Unit,
): Network {
    val channels = mutableMapOf<String, Channel>()
    val nodes = mutableMapOf<String, Node>()
    val network = Network(channels, nodes)

    // load describeGraphJsonFile
    val json = ObjectMapper().readValue(
        describeGraphJsonFile,
        object : TypeReference<Map<String, List<Map<String, Any>>>>() {},
    )
    json["nodes"]?.forEachIndexed { index, map ->
        val id = map["pub_key"] as String
        nodes[id] = Node(id, network)
        onProgressChanged(index.toFloat() / json["nodes"]!!.size, "Loading nodes from ${describeGraphJsonFile.name}")
    }
    json["edges"]?.forEachIndexed { index, map ->
        val id = if((map["channel_id"] as String).contains(":")) {
            convertShortChannelId((map["channel_id"] as String).toLong())
        }else{
            map["channel_id"] as String
        }
        val capacity = (map["capacity"] as String).toLong()
        val node1 = nodes[map["node1_pub"]] ?: return@forEachIndexed
        val node2 = nodes[map["node2_pub"]] ?: return@forEachIndexed
        channels[id] = Channel(id, node1, node2, capacity, network)
        onProgressChanged(index.toFloat() / json["edges"]!!.size, "Loading edges in ${describeGraphJsonFile.name}")
    }

    // load channelUpdateCSVFile
    var maxLine = Files.lines(Paths.get(channelAnnouncementCSVFile.path)).count()
    var currentLine = 0L
    BufferedReader(FileReader(channelAnnouncementCSVFile)).use { br ->
        var line: String?

        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${channelAnnouncementCSVFile.name}")
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

    maxLine = Files.lines(Paths.get(channelUpdateCSVFile.path)).count()
    currentLine = 0L
    BufferedReader(FileReader(channelUpdateCSVFile)).use { br ->
        var line: String?

        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${channelUpdateCSVFile.name}")
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

    var count = 0
    network.channels.forEach {
        onProgressChanged(count++.toFloat() / network.channels.size, "Initializing channel_update Charts")
        it.value.initChannelUpdateChart()
    }

    estimateDemand(
        network,
        onProgressChanged = {
            onProgressChanged(it, "Estimating demands")
        },
    )

    return network
}

// load CLoTH outputs csv file
fun genGroundTruthNetworkFromSimulatorOutput(
    edgesOutputCSVFile: File,
    nodesOutputCSVFile: File,
    channelsOutputCSVFile: File,
    paymentsOutputCSVFile: File,
    onProgressChanged: (progress: Float, processTitle: String) -> Unit
): Network {
    val channels = mutableMapOf<String, Channel>()
    val nodes = mutableMapOf<String, Node>()
    val network = Network(channels, nodes)

    val edges = mutableMapOf<String, EdgeOutput>()

    var maxLine = Files.lines(Paths.get(edgesOutputCSVFile.path)).count()
    var currentLine = 0L
    BufferedReader(FileReader(edgesOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${edgesOutputCSVFile.name}")
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

    maxLine = Files.lines(Paths.get(nodesOutputCSVFile.path)).count()
    currentLine = 0L
    BufferedReader(FileReader(nodesOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${nodesOutputCSVFile.name}")
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

    maxLine = Files.lines(Paths.get(channelsOutputCSVFile.path)).count()
    currentLine = 0L
    BufferedReader(FileReader(channelsOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${channelsOutputCSVFile.name}")
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

    maxLine = Files.lines(Paths.get(paymentsOutputCSVFile.path)).count()
    currentLine = 0L
    BufferedReader(FileReader(paymentsOutputCSVFile)).use { br ->
        var line: String?
        while (br.readLine().also { line = it } != null) {
            onProgressChanged(currentLine++.toFloat() / maxLine, "Loading ${paymentsOutputCSVFile.name}")
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

    return network
}