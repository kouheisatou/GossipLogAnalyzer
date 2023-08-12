package analyzer

class PaymentsOutputAnalyzer : CSVAnalyzer() {
    override fun analyzeCSVLine(lineText: String?) {
        TODO("Not yet implemented")
        // todo CLoTHのシミュレータ出力(payments_output.csv, channels_output.csv, edges_output.csv, nodes_output.csv)を読み込み、nodeとchannelのリストを生成、これを正解データとする
    }

    override fun onAnalyzingFinished() {
        TODO("Not yet implemented")
    }
}