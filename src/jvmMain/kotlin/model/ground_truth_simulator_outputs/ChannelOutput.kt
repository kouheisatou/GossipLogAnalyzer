package model.ground_truth_simulator_outputs

data class ChannelOutput(
    val id: String,
    val edge1: String,
    val edge2: String,
    val node1: String,
    val node2: String,
    val capacity: String,
    val isClosed: String,
) {
}