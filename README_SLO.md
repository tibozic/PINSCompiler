# PINS Compiler
Prevajalnik za izmišljeni jezik, ki je napisan v Javi. <br>
Izdelan v sklopu predmeta PINS (Prevajalnik In Navidezni Stroji) na dodiplomskem
študiju FRI.

## Tehnologije
- Java
- Visitor design pattern

## Programski jezik PINS
[pins.pdf](https://github.com/tibozic/PINSCompiler/blob/master/pins.pdf)

## Primer programov
### Funkcija "less than"
```
fun lt(a: integer, b: integer): logical = a < b
```

### Funkcija za zamenjavo vrednosti v tabeli
```
fun swap(tab : arr[20] integer, i : integer, j : integer) : integer = (
    {tmp = tab[i]},
    {tab[i] = tab[j]},
    {tab[j] = tmp}
) {where var tmp : integer}
```

### Implementacija quicksort
```
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
) {
    where var pivot: integer; 
    var i: integer; 
    var tmp: integer
};

fun sort(tab: arr[100] integer, low: integer, high: integer) : integer = (
    ({ if low < high then (
        { pi = partition(tab, low, high)},
        sort(tab, low, pi-1),
        sort(tab, pi+1, high)
        ) {where var pi: integer}
    },0)
)
```
