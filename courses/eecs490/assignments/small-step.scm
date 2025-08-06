(define (small-step config)
  (match config
    (($conf (cons cmd rest-cmds) stack)
     (match cmd
       ((@num n)
	($conf rest-cmds (cons ($num n) stack)))
       ((@pop)
	(match stack
          ((cons _ rest-stk) ($conf rest-cmds rest-stk))
	  (_ (stuck "pop"))))))))
