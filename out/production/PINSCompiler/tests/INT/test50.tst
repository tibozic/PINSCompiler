fun main(x: integer): integer = (
  { x = 1 },
  { w = 4 },
  g(2),
  print_str(str_test('aaaaa')),
  0
) { where
  var w: integer;
  fun g(y: integer): integer = (
    { ww = 5 },
    f(3),
    0
  ) { where
    var ww: integer;
    fun f(z: integer): integer = (
      print_int(x),
      print_int(y),
      print_int(z),
      print_int(w),
      print_int(ww),
      0
    )
  }
};


fun str_test(x: string): string = (
  { s = 'lolmao' },
  print_str(s),
  print_str(x),
  print_str('hello'),
  'bruh'
) { where
  var s: string
}
