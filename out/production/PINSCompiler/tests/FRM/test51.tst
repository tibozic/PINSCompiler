fun main(args:arr[10]int):int = (
    # sesteje vsa stevila v argumentih
    {sum = 0},
    {for i = 0, 10, 1:
        {sum = sum + args[i]}
    },
    sum
) {where
    var sum:int;
    var i:int
};

typ int:integer;
typ str:string;
typ bool:logical
