/**
 * Created by root on 8/14/17.
 */
class Feeder {

    static void main(String[] args) {
//        def config = [
//                [
//                        index : 0,
//                        path  : 'DatabaseOperations/SelectUsers?recordsCnt=1500',
//                        weight: 0.5f
//                ],
////                [
////                        index : 1,
////                        path  : 'DatabaseOperations/SelectUsers?recordsCnt=2500',
////                        weight: 0.35f
////                ],
////                [
////                        index : 2,
////                        path  : 'DatabaseOperations/SelectUsers?recordsCnt=4500',
////                        weight: 0.1f
////                ],
//                [
//                        index : 1,
//                        path  : 'DatabaseOperations/SelectUsers?recordsCnt=7000',
//                        weight: 0.05f
//                ]
//        ]
//        def scenarioNumber = args.size() > 2 ? args[2]?.toInteger() : null
//        if (args.size() > 6) {
//            def loads = args[6].split(',')
//            PoissonDataSource.numberOfUsersPerRound = loads.collect { it.toInteger() }
//        }
//        if (args.size() > 5) {
//            config.find { it.index == scenarioNumber }.weight = args[5].toFloat()
//
//        }
//        if (args.size() > 3)
//            PoissonDataSource.lengthOfEachRound = args[3]?.toInteger()
//        def dataSource = new PoissonDataSource(config, scenarioNumber)
//        dataSource.feed(args[0]?.toInteger(), args[1], scenarioNumber, "http://192.180.1.1/", args.size() > 4 ? args[4]?.toInteger() : null)

//        NASADataSource.feed(100)
        ZanbilDataSource.feed(args[0], args[1]?.toInteger(), args[2]?.toInteger(), args[3], args[4])

        println "FINISHED"
    }
}
