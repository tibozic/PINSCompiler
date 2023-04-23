# https://open.kattis.com/problems/server
# standard input is given as program arguments
# standard output is a return statement at the end of the main function

typ int:integer;
typ str:string;
typ bool:logical;

fun main(args:arr[52]int):int = (
    {n = args[0]},
    {t = args[1]},
    {returnValue = getNumberOfTasks(n, t, args)},
    returnValue
) { where
    var n:int;
    var t:int;
    var returnValue:int;

    # functions
    fun getNumberOfTasks(n:int, t:int, args:arr[52]int):int = (
        # {tasks = getTasks(n, args)},
        # zmer pozabm, da ne morm arrayou prirejat
        {sum = 0},
        {count = 0},
        { for i = 0, n, 1: (
            {sum = sum + args[i+2]},
            { if sum <= t then
                {count = count + 1}
            }
        )},
        count
    ) { where
        var tasks:arr[50]int;
        var sum:int;
        var count:int;
        var i:int;

        # functions
        fun getTasks(n:int, tasks:arr[52]int):arr[50]int = (
            { for i = 0, n, 1:
                {out[i] = tasks[i+2]}
            },
            out
        ) { where
            var out:arr[50]int
        }
    }
}
