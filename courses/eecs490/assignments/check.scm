(define check
  (lambda (v1 v2)
    (cond 
      ((equal? (eval v1 (nearest-repl/environment)) v2) #t)
      (else (begin (write-string "error: ")
                   (write v1)
                   (write-string " != ")
                   (write v2)
                   (newline)
                   #f)))))
