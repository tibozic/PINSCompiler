fun main(x: integer): integer = (
  ( {for i = 0, 10, 1:
      {if i % 2 == 0 then
          print('1')
      }
    },
    12
  ) {where var i: integer}
);

fun print(s : string): integer = 0
