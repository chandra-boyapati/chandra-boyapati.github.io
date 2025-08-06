;;;;; These examples are taken from the book "The Little Schemer"
;;;;; by Daniel P. Friedman and Matthias Felleisen


; Not a part of scheme, but required for the examples

(define atom?
  (lambda (x)
    (and (not (pair? x)) (not (null? x)))))

;;;;;

(define lat?
  (lambda (l)
    (cond
     ((null? l) #t)
     ((atom? (car l)) (lat? (cdr l)))
     (else #f))))

(define member?
  (lambda (a lat)
    (cond
     ((null? lat) #f)
     (else (or (eq? (car lat) a) (member? a (cdr lat)))))))

(define rember
  (lambda (a lat)
    (cond
     ((null? lat) (quote ()))
     ((eq? (car lat) a) (cdr lat))
     (else (cons (car lat) (rember a (cdr lat)))))))

(define insertR
  (lambda (new old lat)
    (cond
     ((null? lat) (quote ()))
     ((eq? (car lat) old) (cons old (cons new (cdr lat))))
     (else (cons (car lat) (insertR new old (cdr lat)))))))

(define insertL
  (lambda (new old lat)
    (cond
     ((null? lat) (quote ()))
     ((eq? (car lat) old) (cons new (cons old (cdr lat))))
     (else (cons (car lat) (insertL new old (cdr lat)))))))

(define subst
  (lambda (new old lat)
    (cond
     ((null? lat) (quote ()))
     ((eq? (car lat) old) (cons new (cdr lat)))
     (else (cons (car lat) (subst new old (cdr lat)))))))

;;;;;

(define rember*
  (lambda (a l)
    (cond
     ((null? l) (quote ()))
     ((atom? (car l))
      (cond
       ((eq? (car l) a) (rember* a (cdr l)))
       (else (cons (car l) (rember* a (cdr l))))))
     (else (cons (rember* a (car l)) (rember* a (cdr l)))))))

;;;;;

(define eqan?
  (lambda (a1 a2)
    (cond
     ((and (number? a1) (number? a2)) (= a1 a2))
     ((or (number? a1) (number? a2)) #f)
     (else (eq? a1 a2)))))

;;;;; First cut

(define eqlist?
  (lambda (l1 l2)
    (cond
     ((and (null? l1) (null? l2)) #t)
     ((or  (null? l1) (null? l2)) #f)
     ((and (atom? (car l1)) (atom? (car l2)))
      (and (eqan? (car l1) (car l2)) (eqlist? (cdr l1) (cdr l2))))
     ((or  (atom? (car l1)) (atom? (car l2))) #f)
     (else (and (eqlist? (car l1) (car l2)) (eqlist? (cdr l1) (cdr l2)))))))

;;;;; Note: this overwrites the standard library equal?

(define equal?
  (lambda (s1 s2)
    (cond
     ((and (atom? s1) (atom? s2)) (eqan? s1 s2))
     ((or (atom? s1) (atom? s2)) #f)
     (else (eqlist? s1 s2)))))

;;;;; Mutually recursive version

(define eqlist?
  (lambda (l1 l2)
    (cond
     ((and (null? l1) (null? l2)) #t)
     ((or (null? l1) (null? l2)) #f)
     (else (and (equal? (car l1) (car l2)) (eqlist? (cdr l1) (cdr l2)))))))

;;;;;

(define rember2
  (lambda (s l)
    (cond
     ((null? l) (quote ()))
     ((equal? (car l) s) (cdr l))
     (else (cons (car l) (rember s (cdr l)))))))

;;;;; Generalizing rember and rember2 into rember-f

(define rember-f
  (lambda (test?)
    (lambda (a l)
      (cond
       ((null? l) (quote ()))
       ((test? (car l) a) (cdr l))
       (else (cons (car l) (rember-f test? a (cdr l))))))))

(define rember  (rember-f eq?))

(define rember2 (rember-f equal?))

;;;;; Generalizing insertL and insertR to insert-g

(define insert-g
  (lambda (seq)
    (lambda (new old l)
      (cond
       ((null? l) (quote ()))
       ((eq? (car l) old) (seq new old (cdr l)))
       (else (cons (car l) ((insert-g seq) new old (cdr l))))))))

(define seqL
  (lambda (new old l)
    (cons new (cons old l))))

(define seqR
  (lambda (new old l)
    (cons old (cons new l))))

(define insertL (insert-g seqL))

(define insertR (insert-g seqR))

;;;;; Using insert-g to redefine subst and rember

(define seqS
  (lambda (new old l)
    (cons new l)))

(define seqrem
  (lambda (new old l)
    l))

(define subst (insert-g seqS))

(define rember
  (lambda (a l)
    ((insert-g seqrem) #f a l)))

;;;;;

(define multirember
  (lambda (a lat)
    (cond
     ((null? lat) (quote ()))
     ((eq? a (car lat)) (multirember a (cdr lat)))
     (else (cons (car lat) (multirember a (cdr lat)))))))

(define multirember-f
  (lambda (test?)
    (lambda (a lat)
      (cond
       ((null? lat) (quote ()))
       ((test? a (car lat)) ((multirember-f test?) a (cdr lat)))
       (else (cons (car lat) ((multirember-f test?) a (cdr lat))))))))

(define multirember&co
  (lambda (a lat col)
    (cond
     ((null? lat) (col (quote()) (quote ())))
     ((eq? (car lat) a)
      (multirember&co a (cdr lat)
          (lambda (newlat seen) (col newlat (cons (car lat) seen)))))
     (else
      (multirember&co a (cdr lat)
          (lambda (newlat seen) (col (cons (car lat) newlat) seen)))))))

