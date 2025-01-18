# PINS Compiler - [Slovenian README](README_SLO.md)
A compiler for an imaginary language, written in Java. <br>  
Developed as part of the PINS (Compiler and Virtual Machines) course in the undergraduate study at FRI.

## Technologies  
- Java  
- Visitor design pattern

## PINS Programming Language  
[pins.pdf](https://github.com/tibozic/PINSCompiler/blob/master/pins.pdf)

## Example Programs  
### "Less than" Function
```
fun lt(a: integer, b: integer): logical = a < b
```

### Function to Swap Values in an Array
```
fun swap(tab : arr[20] integer, i : integer, j : integer) : integer = (
    {tmp = tab[i]},
    {tab[i] = tab[j]},
    {tab[j] = tmp}
) {where var tmp : integer}
```

### QuickSort Implementation
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
