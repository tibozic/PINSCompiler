fun main( x: integer ) : integer = (
    { if 1 == 1 & 2 == 2 | 3 == 3 | 4 == 4
    then
        (
            { for i = 0, 10, 1:
                (
                    {k = k + i}
                ) { where
                    var k : integer
                }
            }
        ) { where
            var k : integer;
            var i : integer
        }
    else
        (
            { while k < 20:
                (
                    {k = k + 1}
                )
            }
        ) { where
            var k : integer
        }
    }, 0
)
