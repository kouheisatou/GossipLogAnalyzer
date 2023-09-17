package model.ground_truth_simulator_outputs

data class EdgeOutput(
    val id: String,
    val channelId: String,
    val counterEdgeId: String,
    val fromNodeId: String,
    val toNodeId: String,
    val balance: String,
    val feeBase: String,
    val feeProportional: String,
    val minHtlc: String,
    val timelock: String,
    val isClosed: String,
    val totFlows: String,
)