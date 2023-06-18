class Node(val id: String) {
    val channels = mutableListOf<Channel>()

    override fun equals(other: Any?): Boolean {
        return other is Node && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}