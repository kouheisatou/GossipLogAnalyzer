package model.ground_truth_simulator_outputs

data class PaymentOutput(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val amount: String,
    val startTime: String,
    val endTime: String,
    val mpp: String,
    val isSuccess: String,
    val noBalanceCount: String,
    val offlineNodeCount: String,
    val timeoutExp: String,
    val attempts: String,
    val route: List<EdgesOutput>,
    val totalFee: String,
)