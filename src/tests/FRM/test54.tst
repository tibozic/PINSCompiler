fun main(args:arr[10]int):str = (
    # preverimo ce so vsa stevila deljiva z 2 ali s 3 ali z nobenim ali z obema od teh dveh
    {printString = ''},
    {deljivo[0] = preveriArr(args)[0]},
    {deljivo[1] = preveriArr(args)[1]},
    {if deljivo[0] & deljivo[1] then
        {printString = 'Deljivo z 2 in 3'}
    else {if deljivo[0] then
        {printString = 'Deljivo z 2'}
    else {if deljivo[1] then
        {printString = 'Deljivo s 3'}
    else
        {printString = 'Ni deljivo z nobenim od teh'}
    }}},
    printString
) {where
    var printString:str;
    var deljivo:arr[2]bool
};

fun deljiva(n:int, m:int):bool = (
    {returnValue = n % m == 0},
    returnValue
) {where
    var returnValue:bool
};

fun deljivaZ2(n:int):bool = deljiva(n, 2);
fun deljivaS3(n:int):bool = deljiva(n, 3);

fun preveriArr(array:arr[10]int):arr[2]bool = (
    {returnValue[0] = true},
    {returnValue[1] = true},

    {for i = 0, 10, 1: (
        {if !deljivaZ2(array[i]) | !returnValue[0] then
            {returnValue[0] = false}
        },
        {if !deljivaS3(array[i]) | !returnValue[1] then
            {returnValue[1] = false}
        }
    )},

    returnValue
) {where
    var returnValue:arr[2]bool;
    var i:int
};

typ int:integer;
typ str:string;
typ bool:logical
