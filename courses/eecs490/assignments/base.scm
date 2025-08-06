(define (check v1 v2)
  (cond
   ((equal? (eval v1 (nearest-repl/environment)) v2) #t)
   (else (begin (display "error: ")
                (display v1)
                (display " != ")
                (display v2)
                (newline)
                #f))))

; This gets overwritten during evaluation through postfunc

(define stuck '())

(define (postfunc n evaluate)
  (if (or (not (integer? n)) (negative? n))
      (error "first argument must be a nonnegative integer")
      (lambda (arguments)
        (define (all predicate? lst)
          (if (null? lst) #t (and (predicate? (car lst))
                                  (all predicate? (cdr lst)))))
        (let ((arglen (length arguments)))
          (cond
           ((not (= arglen n))
            (error
             (string-append
              (number->string arglen) " argument(s) supplied; "
              (number->string n) " expected")))
           ((not (all integer? arguments))
            (error "all arguments must be integers"))
           (else
            (call-with-current-continuation
             (lambda (exit)
               (set! stuck (lambda (str)
                             (exit (string-append "stuck: " str))))
               (evaluate arguments)))))))))

(define-syntax postfix
  (syntax-rules ()
    ((postfix expn exp ...) (postrun expn (quote (exp ...))))))

(define-syntax posttext
  (syntax-rules ()
    ((posttext expn exp ...) (postrun expn (quote (exp ...))))))

(define (postrun n raw-cmds)
  (postfunc n
    (lambda (args)
      (evaluate
        (make-initial-config
          (map parse-command raw-cmds)
          (map $num args))))))
