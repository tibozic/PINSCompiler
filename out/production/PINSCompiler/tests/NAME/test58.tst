fun main(x: integer): integer = (
    ( {for i = 0, 10, 1:
        {if i / 2 == 1 | i & 2 != 0 | 1 == i & 'a' != 'b' & 1 != 3 then
            print(1)
        }
      }
    ) {where var i: integer}
);

fun print(s : string): integer = 0
