(define-datatype command
  (@num int)
  (@seq (listof command))
  (@pop)
  (@swap)
  (@sel)
  (@exec)
  (@arithop (-> (int int) int))
  (@relop   (-> (int int) bool))
  (@nget))

(define (parse-command exp)
  (match exp
    ((int->sexp n) (@num n))
    ((list->sexp lst) (@seq (map parse-command lst)))
    ('pop (@pop))
    ('swap (@swap))
    ('sel (@sel))
    ('exec (@exec))
    ('add (@arithop +))
    ('sub (@arithop -))
    ('mul (@arithop *))
    ('div (@arithop quotient))
    ('mod (@arithop remainder))
    ('lt  (@relop <))
    ('eq  (@relop =))
    ('gt  (@relop >))
    ('nget (@nget))))

(define-datatype configuration
  ($conf (listof command) (listof stack-element)))

(define-datatype stack-element
  ($num int)
  ($seq (listof command)))

(define (make-initial-config commands arguments)
  ($conf commands arguments))

(define (evaluate config)
  (match config
    (($conf () (cons ($num n) _)) n)
    (($conf () _) (stuck "evaluate"))
    (_ (evaluate (small-step config)))))

(define (small-step config)
  (match config
    (($conf (cons cmd rest-cmds) stack)
     (match cmd

       ; integer: push on the stack
       ((@num n)
        ($conf rest-cmds (cons ($num n) stack)))

       ; sequence: push on the stack
       ((@seq cmds)
        ($conf rest-cmds (cons ($seq cmds) stack)))

       ; pop: remove top of stack
       ((@pop)
        (match stack
          ((cons _ rest-stk) ($conf rest-cmds rest-stk))
          (_ (stuck "pop"))))

       ; swap: switch top two elements of stack
       ((@swap)
        (match stack
          ((cons a (cons b rest-stk))
           ($conf rest-cmds (cons b (cons a rest-stk))))
          (_ (stuck "swap"))))

       ; sel: if third is zero, then first, else second
       ((@sel)
        (match stack
          ((cons a (cons _ (cons ($num 0) rest-stk)))
           ($conf rest-cmds (cons a rest-stk)))
          ((cons _ (cons b (cons ($num _) rest-stk)))
           ($conf rest-cmds (cons b rest-stk)))
          (_ (stuck "sel"))))

       ; TODO: implement nget
       ((@nget)
        (stuck "TODO: implement nget"))

       ; exec: command list added to front
       ((@exec)
        (match stack
          ((cons ($seq s) rest-stk) ($conf (append s rest-cmds) rest-stk))
          (_ (stuck "exec"))))

       ; arithop: two integers, push result
       ((@arithop op)
        (match stack
          ((cons ($num a) (cons ($num b) rest-stk))
           (cond
             ((and (zero? a) (or (eq? op quotient) (eq? op remainder)))
              (stuck "divide-by-zero"))
             (else ($conf rest-cmds (cons ($num (op b a)) rest-stk)))))
          (_ (stuck "arithop"))))

       ; relop: two integers, push result
       ((@relop op)
        (match stack
          ((cons ($num a) (cons ($num b) rest-stk))
           ($conf rest-cmds (cons ($num (if (op b a) 1 0)) rest-stk)))
          (_ (stuck "relop"))))))))
