fun main(args:arr[100]int):int = (
    # najde najbolj pogost element v tabeli
    # tabela ima elemente vrednosti od 1 do 10 vključno

    # napolnimo tabelo pogostosti z ničlami
    {for i = 0, 10, 1:
        {pogostost[i] = 0}
    },

    # preštejemo koliko je katerih elementov
    {for i = 0, 100, 1:
        {pogostost[args[i-1]] = pogostost[args[i-1]] + 1}
    },

    # poiščemo največjo vrednost v tabeli pogostosti
    {max = najvecji(pogostost)},

    # poiščemo indeks največje vrednosti v tabeli pogostosti
    {max_index = index(pogostost, max)},

    # vrnemo največji element
    max_index+1
) {where
    var pogostost:arr[10]int;
    var i:int;
    var max:int;
    var max_index:int;

    # definirane funkcije
    fun najvecji(array:arr[10]int):int = (
        {max = 0},
        {for i = 0, 10, 1:
            {if array[i] > max then
                {max = array[i]}
            }
        },
        max
    ) {where
        var i:int;
        var max:int
    };

    fun index(array:arr[10]int, value:int):int = (
        {returnValue = -1},
        {for i = 0, 10, 1:
            {if array[i] == value & returnValue == -1 then
                {returnValue = i}
            }
        },
        returnValue
    ) {where
        var i:int;
        var returnValue:int
    }
};

typ int:integer;
typ str:string;
typ bool:logical
