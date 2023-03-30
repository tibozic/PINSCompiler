# quicksort
fun partition (tab: arr[100] integer, low: integer, high: integer) : integer = (
    {pivot = tab[high]},
    {i = (low - 1)},
    ({for j = low, high, i:
        { if tab[j] <= pivot then (

            {i = i+1},
            {tmp = tab[i]},
            {tab[j] = tab[j]},
            {tab[j] = tmp}

        ) {where var tmp: integer}}
    }) {where var j: integer},

    {tmp = tab[i+1]},
    {tab[i+1] = tab[high]},
    {tab[high] = tmp}
) {where var pivot: integer; var i: integer; var tmp: integer};

fun sort(tab: arr[100] integer, low: integer, high: integer) : integer = (
    {if low < high then (
        {pi = partition(tab, low, high)},
        sort(tab, low, pi-1),
        sort(tab, pi+1, high)
    ) {where var pi: integer}}
)
