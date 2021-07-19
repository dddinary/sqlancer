package riffle;

import sqlancer.Main;

public class Bootstrap {
    public static void main(String[] args) {
//        Main.executeMain(
//                "--random-seed", "21932",
//                // "--timeout-seconds", "100",
//                "--num-queries", "1",
//                //"--print-statements", "true",
//                "--print-progress-information", "false",
//                "--max-num-inserts", "50",
//                "--num-tries", "1", "--max-generated-databases", "1",
//                "--username", "root", "--password", "tobeno.1", "--database-prefix", "sqlancer",
//                "--num-threads", "1", "mysql", "--oracle", "TXN");
        Main.executeMain(
                "--print-statements", "true",
                "--username", "root", "--password", "tobeno.1", "--database-prefix", "sqlancer",
                "mysql", "--oracle", "PQS");
    }
}
