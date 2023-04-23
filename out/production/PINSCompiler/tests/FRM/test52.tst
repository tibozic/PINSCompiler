fun main(args:arr[10]int):int = (
    # najdi najvecjega in najmanjsega
    # izpisi razliko
    # stevila so med -10000 in 10000

    {max = najvecji(args)},
    {min = najmanjsi(args)},
    {razlika = max - min},
    razlika
) {where
    var max:int;
    var min:int;
    var razlika:int
};

fun najvecji(array:arr[10]int):int = (
    {max = -10000},
    {for i = 0, 10, 1:
        {if array[i] > max then
            {max = array[i]}
        }
    },
    max
) {where
    var max:int;
    var i:int
};

fun najmanjsi(array:arr[10]int):int = (
    {min = 10000},
    {for i = 0, 10, 1:
        {if array[i] < min then
            {min = array[i]}
        }
    },
    min
) {where
    var min:int;
    var i:int
};

typ int:integer;
typ str:string;
typ bool:logical
