fun main(x: integer): integer = (
    { s = 'str1' },
    print_str(s),
    print_str('str2'),
    print_str(f('str3')),
    0
) { where 
    var s: string;
    fun f(str: string): string = (
        print_str(str),
        'str4'
    )
}
