;; This Scheme+ Version 1.2 
;; Built on Wed Sep 14 16:47:44 EDT 1994
;; Includes:
;;    macros.scm
;;    constructor.scm
;;    datatype.scm
;;    match.scm
;;    top-level.scm
 
(declare (usual-integrations))
 
(define scheme+/version "1.2")
 
;;---------------------------------------------------
;; macros.scm
;;; ---------------------------------------------------------------------------
;;; Macro definitions

;;; Scheme+ macros need to be visible both at run-time and a compile-time
;;;
;;; BJR: put define-scheme+-macro in user-initial-syntax-table so that
;;; we can load datatype.scm and match.scm independently.
;;;
;;; amdragon: this definition of define-scheme+-macro should work with
;;; the new macro system used in MIT scheme 7.7.  These macros are
;;; non-hygienic, so this emulates the R5RS syntax pattern matching,
;;; but does so through an sc-macro-transformer.  Note that syntax
;;; tables are no longer used, but care is taken with regular and
;;; reversed syntactic closures to emulate the environment behavior of
;;; the old macros.
(define-syntax define-scheme+-macro
  ;;; Use a (non-reversed) syntactic closure transformer here because,
  ;;; if I understand the old macros correctly, great care was taken
  ;;; to define every macro everywhere, which this will achieve,
  ;;; regardless of where the scheme+ macro is defined
  (sc-macro-transformer
   (lambda (exp usage-env)
     ;;; Transforms a pattern into a let-bindings expression, where
     ;;; each binding is named by its corresponding pattern variable.
     ;;; exp-name is (initially) the symbol naming the pattern in the
     ;;; let
     (define (pattern-bindings pattern exp-name)
       (cond ((null? pattern) ())
	     ((symbol? pattern) `((,pattern ,exp-name)))
	     ((pair? pattern)
	      (append
	       (pattern-bindings (car pattern) (list 'car exp-name))
	       (pattern-bindings (cdr pattern) (list 'cdr exp-name))))))

     ;;; Expand to form an expression that will create the underlying
     ;;; Scheme macro corresponding to this Scheme+ macro
     (let ((name (caadr exp))
	   (pattern (cdadr exp))
	   (body (cddr exp)))
       `(define-syntax ,name
	  ;;; Use a reverse syntactic closure here because the
	  ;;; old-style macro system evaluated macro expansions under
	  ;;; the usage environment
	  (rsc-macro-transformer
	   (lambda (exp transformer-env)
	     (let ((operands (cdr exp)))
	       (let ,(pattern-bindings pattern 'operands)
		 ,@body)))))))))

;;; Get real scheme value in expanded code
(define-scheme+-macro (get-real identifier)
  `(access ,identifier ()))


;;; ---------------------------------------------------------------------
;;; Error handling macros - need to be visible both in this file
;;; and at run time.
;; BJR - These are now scheme procedures

; (define-macro (define-macro-both pattern . body)
;   `(begin
;      (define-macro ,pattern ,@body)
;      (syntax-table-define
; 	 user-initial-syntax-table
; 	 ',(car pattern)
;        (macro ,(cdr pattern) ,@body))))
; 
; (define-macro-both (syntax-error string . rest)
;   `(error (string-append "\nSCHEME+ SYNTAX ERROR: " ,string)
;           ,@(if rest rest '())))
; 
; (define-macro-both (runtime-error string . rest)
;   `((get-real error) 
;     ((get-real string-append) "\nSCHEME+ RUN-TIME TYPE ERROR!\n"
; 			      ,string)
;           ,@(if rest rest '())))

(define (scheme+/syntax-error msg . rest)
  (error (apply error-string "\nSCHEME+ SYNTAX ERROR: " msg rest)
	 
	 rest))

(define (scheme+/runtime-error msg . rest)
  (error (apply error-string "\nSCHEME+ RUN-TIME TYPE ERROR!\n" msg rest)
	 rest))




;; From mini-fx/version-4.2.7.scm

(define (error-string str msg . objs )
  (string-append 
   str
   msg
   "\n"
   (objects->string objs)
   "\n"
   ))


(define objects->string 
  (let ((separator "------------------------------------------------------------"))
    (lambda (objs)
      (if (null? objs)
	  ""
	  (string-append
	   separator
	   (apply 
	    string-append
	    (map
	     (lambda (obj)
	       (string-append
		(with-output-to-string (lambda () (pp obj)))
		"\n"
		separator
		))
	     objs)))))))
 
;;---------------------------------------------------
;; constructor.scm
;;;----------------------------------------------------------------------------
;;; Constructors

;;; reistad 8/19/94 -- added quote and quasiquote s-expression consructors
;; 			from mini-fx.scm (1991)



(define constructor-tag   '(constructor))

(define (scheme+/make-constructor bundler unbundler)
  (make-apply-hook bundler 
		   (list constructor-tag unbundler)))
(define make-constructor scheme+/make-constructor)

(define (constructor? obj)
  (and (apply-hook? obj)
       (let ((extra (apply-hook-extra obj)))
	 (and (list? extra)
	      (= (length extra) 2)
	      (eq? (first extra) constructor-tag)))))


(define (bundler obj)
  (if (constructor? obj)
      obj
      (error "bundler: Not a constructor!" obj)))

(define (unbundler obj)
  (if (constructor? obj)
      (second (apply-hook-extra obj))
      (error "unbundler: Not a constructor!" obj)))

;;;----------------------------------------------------------------------------
;;; Simulated LIST datatype

(define null
  (make-constructor 
   (lambda () '())
   (lambda (obj succ fail)
     (if (null? obj) 
	 (succ)
	 (fail)))))

(define cons
  (make-constructor
   (access cons system-global-environment)
   (lambda (obj succ fail)
     (if (pair? obj) 
	 (succ (car obj) (cdr obj))
	 (fail)))))

; (define null
;   (make-constructor 
;    (lambda () '())
;    (lambda (obj succ fail)
;      (if *datatype-paranoid-match?*  ; See DATATYPE.SCM
; 	 (if (not (or (null? obj) (pair? obj)))
; 	     (error "Deconstructor for NULL given a non list")))
;      (if (null? obj) 
; 	 (succ)
; 	 (fail)))))
; 
; (define cons
;   (make-constructor
;    (access cons ())
;    (lambda (obj succ fail)
;      (if *datatype-paranoid-match?*  ; See DATATYPE.SCM
; 	 (if (not (or (null? obj) (pair? obj)))
; 	     (error "Deconstructor for CONS given a non list")))
;      (if (pair? obj) 
; 	 (succ (car obj) (cdr obj))


; This can't work without the deconstructor being passed the 
; number of args given to the deconstructor (or a datum from 
; which that number could be constructed).
; 
; (define list
;   (make-constructor
;    (access list ())
;    (lambda (obj succ fail)
;      (if (list? obj)
; 	 (apply succ obj)
; 	 (fail)))))
     
;;;----------------------------------------------------------------------------
;;; Simulated S-EXPRESSION datatype

(define (make-sexp-constructor sym pred)
  (make-constructor
   (lambda (sexp)             ; Constructor does a type check with PRED
     (if (pred sexp)
	 sexp
	 (error "Sexp constructor -- incorrect type:" sym sexp)))
   (lambda (sexp succ fail)   ; Deconstructor does a type check with PRED
     (if (pred sexp)
	 (succ sexp)
	 (fail)))))

(define int->sexp  (make-sexp-constructor 'int  integer?))
(define real->sexp (make-sexp-constructor 'real real?))
(define bool->sexp (make-sexp-constructor 'bool boolean?))
(define char->sexp (make-sexp-constructor 'char char?))
(define string->sexp (make-sexp-constructor 'string string?))
(define symbol->sexp (make-sexp-constructor 'symbol symbol?))
(define list->sexp   (make-sexp-constructor 'list   list?))
(define vector->sexp (make-sexp-constructor 'vector vector?))




;; reistad 8/24/94 -- added for orthogonality/uniformity with Scheme

(define (scheme+/make-check-type-constructor sym pred?)
  (make-constructor 
   (lambda (obj) (if (pred? obj) 
		     obj
		     (error "Object was not of correct type:" sym obj)))
   (lambda (obj succ fail)
     (if (pred? obj)
	 (succ obj)
	 (fail)))))

(define a-number    (scheme+/make-check-type-constructor 'number number?))
(define an-integer  (scheme+/make-check-type-constructor 'integer integer?))
(define a-real      (scheme+/make-check-type-constructor 'real real?))
(define a-complex   (scheme+/make-check-type-constructor 'complex complex?))
(define a-rational  (scheme+/make-check-type-constructor 'rational rational?))
(define a-char      (scheme+/make-check-type-constructor 'char char?)) 
(define a-boolean   (scheme+/make-check-type-constructor 'boolean boolean?))
(define a-symbol    (scheme+/make-check-type-constructor 'symbol symbol?))
(define a-procedure (scheme+/make-check-type-constructor 'procedure procedure?))
(define a-vector    (scheme+/make-check-type-constructor 'vector vector?))
(define a-list      (scheme+/make-check-type-constructor 'list list?))
(define a-string    (scheme+/make-check-type-constructor 'string string?)) 



;;; reistad 8/19/94 -- taken from mini-fx (1991) for low level macro system
;;;
;;; Additional procedures for manipulating s-expressions with QUOTE,
;;; QUASIQUOTE, UNQUOTE, and UNQUOTE-SPLICING.

(define (scheme+/make-quote-constructor sym)
  (make-constructor
   (lambda (x) (list sym x))		;; Constructor tags item
   (let ((pred (lambda (thing)		;; Deconstructor checks tag
		 (and (pair? thing)
		      (eq? (car thing) sym)
		      (pair? (cdr thing))
		      (null? (cddr thing))))))
     (lambda (thing succ fail)
       (if (pred thing)
	   (succ (cadr thing))
	   (fail))))))

(define quoted->sexp            (scheme+/make-quote-constructor 'quote))
(define quasiquoted->sexp	(scheme+/make-quote-constructor 'quasiquote))
(define unquoted->sexp          (scheme+/make-quote-constructor 'unquote))
(define unquoted-splicing->sexp (scheme+/make-quote-constructor 'unquote-splicing))

 
;;---------------------------------------------------
;; datatype.scm
;;;----------------------------------------------------------------------------
;;; DATATYPE.SCM
;;;
;;; Author: Lyn
;;; Log:
;;; * 8/19/94 - reistad: Eliminated use of define-syntax so define-datatype 
;;;		macro is now in match.scm.  Also dropped use of dtyp-desc as
;;;		Scheme+ ingnores all type crap.
;;;	Define-Datatype now defines the constructor name to be a package 
;;;	  of both a bundler and unbundler as we've eliminated ~.  The 
;;;       constructor is behaves just like the bundler but has a way to
;;;       get to the unbundler.
;;; * 8/11/94 - Updated
;;; * 6/20/94 - Created
;;; 
;;; Notes:
;;; Implements datatypes for Scheme+. There is a design choice here that 
;;; doesn't exist for Mini-FX: what should an unbundler do when not given
;;; a discriminant of the expected type? E.g., suppose BAR is a constructor
;;; for datatype FOO, and P is an instance of the CONS datatype. Should
;;; 
;;;    ((unbundler bar) p <succeed> <fail>) 
;;; 
;;; signal an error, or merely call <fail>?  The latter is more in the 
;;; "Scheme tradition", but the former provides superior error messages
;;; (otherwise, the only error is the largely unuseful "No pattern matched!").
;;; The latter makes sense if we view MATCH as being instantiated to a 
;;; particular datatype before we apply it.
;;; 
;;; Since its not clear which version is The Right Thing, I provide a flag
;;; *DATATYPE-PARANOID-MATCH?* that controls how this situation is handled.
;;; By default, this is #t -- i.e., all desconstructors in a given match
;;; are assumed to be for the same datatype. 
;;; 
;;;----------------------------------------------------------------------------

;; see NOTES above
(define *datatype-paranoid-match?* #t)




;;; $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
;;; DEFINE-DATATYPE Begin
;;;
;;; DEFINE-DATATYPE declares a sum-of-products data structure
;;; and creates the associated constructors.
;;;
;;; NOTE: the implementation below uses the same name for both the constructor
;;; and deconstructor.  (Actually uses application hooks to store the 
;;; deconstructor on the constructor.) -- ACTUALLY, renamed so that
;;; always use constructor to refer to bundler/unbundler pair.
;;; 
;;; E.g. 
;;; 
;;;   (define-datatype int-tree
;;;     (int-leaf int)
;;;     (int-node int-tree int-tree))
;;; 
;;; expands into				
;;; 
;;;   (define int-leaf (make-constructor
;;;			 (lambda (i) ...)		;; constructor
;;;			 (lambda (t succ fail) ...)))	;; deconstructor
;;;   (define int-node (make-constructor
;;;			 (lambda (t1 t2) ...)		;; constructor
;;;			 (lambda (t succ fail) ...)))	;; deconstructor
;;;
;;; where
;;; 
;;;   (int-leaf 0) -> #[int-tree:int-leaf 0]
;;;
;;;   (int-node (int-leaf 0) (int-leaf 1)) ->
;;;         #[int-tree:int-node
;;;  	      #[int-tree:int-leaf 0]
;;; 	      #[int-tree:int-leaf 1]]
;;; 
;;; Here's a sample procedure using INT-TREE. Note the use of
;;; deconstructors in the patterns of MATCH (eventhough there's no ~s)
;;; 
;;;   (define (int-tree-sum t)
;;;     (match t
;;;       ((int-leaf n) n)
;;;       ((int-node left right) (+ (tree-sum left) (tree-sum right)))))
;;; 
;;;
;;; DEFINE-DATATYPE can be parameterized by type as well:
;;; (Yeah, but all types are ignored -- documentation only.
;;; 
;;;   (define-datatype (tree t)
;;;     (leaf t)
;;;     (node (tree t) (tree t)))
;;; 
;;;DROP THIS?
;;; These type parameters are (conceptually) parameters of the constructors
;;; and deselectors, but can be thought of as being removed by 
;;; implicit projection.  E.g. the above "expands" into
;;; 
;;;   ; Constructors
;;;   (define leaf (plambda (t) (lambda (i) ...)
;;;   (define node (plambda (t) (lambda (t1 t2) ...)
;;;   ; Deconstructors
;;;   (define leaf~ (plambda (t) (lambda (t success-cont fail-cont) ...))
;;;   (define node~ (plambda (t) (lambda (t success-cont fail-cont) ...))
;;; 
;;; but the t's never have to be specified:
;;;  
;;;   (define (tree-depth t)
;;;     (match t
;;;       ((leaf t) 0)
;;;       ((node left right) (1+ (max (tree-depth left) (tree-depth right))))
;;;
;;;       ((node left right) (1+ (max (tree-depth left) (tree-depth right))))))
;;;
;;;

;;; ---------------------------------------------------------------------------
;;; Begin DEFINE-DATATYPE code

;;; ---------------------------------------------------------------------------
;;; The DEFINE-DATATYPE macro

;;; There are two possible forms for DEFINE-DATATYPE:
;;;   
;;; (define-datatype I (I D*)*)		; Straightforward datatype
;;; (define-datatype (I I*) (I D*)*)	; Parameterized datatype
;;;
;;; Here we ignore any params if they occur.
;;;
;;; In this implementation we ignore types altogether and simply simulate the 
;;; run-time behavior of DEFINE-DATATYPE.


;;; Begin DEFINE-DATATYPE macro
(define-scheme+-macro (define-datatype name-or-name+params . sum-of-products)

  (define (datatype-name header)
    (cond 
     ((symbol? header)	header)         ; (define-datatype I (I D*)*)
     ((and (pair? header)               ; (define-datatype (I I*) (I D*)*)
	   (every? symbol? header))
      (car name-or-name+params))
     (else				; otherwise is not a legal define-datatype
      (scheme+/syntax-error "Ill-formed DEFINE-DATATYPE header: " header))
     ))

  (define (datatype-clause-names-and-params clause-list receiver)
    (if (null? clause-list)
	(receiver '() '())
	(let ((first-clause (car clause-list)))
	  (if (and (list? first-clause)
		   (>= (length first-clause) 1))
	      (datatype-clause-names-and-params 
	       (cdr clause-list)
	       (lambda (names params)
		 (receiver 
		  (cons (check-clause-name (car (car clause-list))) 
			names)
		  (cons (map check-clause-param (cdr (car clause-list))) 
			params))))
	      (scheme+/syntax-error 
	       (string-append 
		"Ill-formed DEFINE-DATATYPE clause.\n"
		"(Clauses must be of the form: (<constructor-name> <type_1> ... <type_n>) ):\n"
		)
	       first-clause)))))

  (define (check-clause-name obj)
    (if (symbol? obj)
	obj
	(scheme+/syntax-error 
	 "Non-symbolic constructor name within a DEFINE-DATATYPE clause:\n"
	 obj)))

  ;; *** Need to have this do real error checking in future
  (define (check-clause-param type) type)

  (define (every? test lst)
    (if (null? lst) 
	#t
	(and (test (car lst))
	     (every? test (cdr lst)))))

  ; Main body of DEFINE-DATATYPE macro
  (let ((dname (datatype-name name-or-name+params)))
    (datatype-clause-names-and-params sum-of-products
      (lambda (clause-names clause-params)
	`(begin 
	   ;; Since Scheme+ has a single namespace, we must bind the datatype
	   ;; name to *some* kind of object in order to preserve the semantics
	   (DEFINE ,dname ,(make-datatype-descriptor dname)) ;;? clause-names
	   ,@(map (lambda (cname cparams)
		    `(DEFINE ,cname
		       (scheme+/make-constructor
			(scheme+/make-datatype-bundler ',dname 
						       ',cname 
						       ,(length cparams))
			(scheme+/make-datatype-unbundler ',dname 
							 ',cname))))
		  clause-names
		  clause-params)
	   ,dname))))
    ) 
;;; End DEFINE-DATATYPE macro


;;; ---------------------------------------------------------------------------
;;; DEFDATATYPE is a synonym for DEFINE-DATATYPE

(define-scheme+-macro (defdatatype name-or-name+params . sum-of-products)
  `(DEFINE-DATATYPE ,name-or-name+params ,@sum-of-products))



;; helper procedures 

(define (scheme+/make-datatype-bundler dtyp-name cnstr-name nargs)
  (lambda args
    (if (= nargs (length args))
	(make-datatype-instance dtyp-name dtyp-name cnstr-name args)
	(error "Incorrect number of arguments given to bundler"
	       cnstr-name
	       args))))
(define make-datatype-bundler scheme+/make-datatype-bundler)

(define (scheme+/make-datatype-unbundler dtyp-name cnstr-name)
  (lambda (obj succ fail)
    (if *datatype-paranoid-match?*  ; Could move this outside if 
	                            ; wanted creation-time choice.
	;; This case signals an error if OBJ isn't exactly right.
	(scheme+/ensure-datatype dtyp-name cnstr-name obj)
	obj)
    (if (and (datatype-instance? obj)
	     (eq? dtyp-name (datatype-instance-descriptor obj))
	     (eq? cnstr-name (datatype-instance-constructor obj)))
	(apply succ (datatype-instance-args obj))
	(fail))))
(define make-datatype-unbundler scheme+/make-datatype-unbundler)

(define (scheme+/ensure-datatype dtyp-name cnstr-name obj)
  (cond 
   ((not (datatype-instance? obj))
    (error (string-append "Unbundler "
			  (symbol->string cnstr-name)
			  " for datatype "
			  (symbol->string dtyp-name)
			  " applied to non-datatype instance: ")
	   obj))
   ((not (eq? dtyp-name (datatype-instance-descriptor obj)))
    (error (string-append "Unbundler " 
			  (symbol->string cnstr-name)
			  " for datatype "
			  (object->string dtyp-name)
			  " applied to instance of datatype "
			  (object->string (datatype-instance-descriptor obj))
			  ": ")
	   obj))
   (else obj)))


(define (print-datatype-descriptor state desc)
  (unparse-string state "#[datatype ")
  (unparse-object state (datatype-descriptor-name desc))
  (unparse-string state "]"))

(define (print-datatype-instance state instance)
  (let ((type-name (datatype-instance-type-name instance))
	(constructor (datatype-instance-constructor instance))
	(args (datatype-instance-args instance)))
    (unparse-string state "#[")
    (unparse-object state type-name)
    (unparse-string state ":")
    (unparse-object state constructor)
    (for-each (lambda (arg)
		(unparse-string state " ")
		(unparse-object state arg))
	      args)
    (unparse-string state "]")
    ))

(define (object->string obj)
  (with-output-to-string 
    (lambda () (display obj))))
    

;;; Structures

(define-structure (datatype-descriptor 
		   (print-procedure print-datatype-descriptor))
  name)

(define-structure (datatype-instance 
		   (print-procedure print-datatype-instance))
  descriptor
  type-name
  constructor 
  args)




    



 
;;---------------------------------------------------
;; match.scm
;;; DEFINE-DATATYPE and MATCH

;;; Log:
;;; 
;;; reistad 8/19/94 -- Taken from /zu/lyn/6821/fall-91/code/mini-fx.scm
;;;	  to replace original Scheme+ implementation with define-syntax.
;;;	Now use low level macros like in the past.  
;;;	All global refs in macro output code protected with (get-real <id>)
;;;	  which ensures that they cannot be shadowed by user definitions.
;;;
;;; reistad & lyn 8/23/94 (or thereabouts) Redesigned the MATCH desugaring 
;;;       to look a lot nicer.  Previous version was verbose and
;;;       tended to shift to the right a lot.
;;;
;;; reistad & lyn 8/24/94 Designed and implemented a WITH-FAIL construct
;;;       that captures the failure continuation at the beginning of 
;;;       a MATCH clause.  Brian extends to handle duplicated pattern 
;;;       by doing a renaming prepass on the pattern and using WITH-FAIL.
;;;
;;; lyn 9/09/94 -- Fixed RENAME-IN-QUASIQUOTE to handle `_' correctly
;;;       within QUASIQUOTED patterns. They were getting renamed when 
;;;       they shouldn't have been. 
;;;
;;; lyn 9/10/94 -- Fixed RENAME-IN-QUASIQUOTE to correctly handle 
;;;       + non-symbol atoms (e.g., null, numbers, strings, etc.) 
;;;       + deconstructions (i.e., applications of constructors in  
;;;         pattern position)
;;;     Previous version would try to rename these and barf if it
;;;       found duplicates!
;;;     I'm still somewhat confused because I don't understand why 
;;;       renamer seems to get called twice on every pattern!


;;; $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
;;; MATCH Begin

;;; This is not the most efficient possible strategy for implementing
;;; ML-style pattern matching.
;;;
;;; (match foo ((make-foo x y) ...) ...)
;;;   ==>  (~match-make-foo foo 2 (lambda (x y) ...) (lambda () ...))
;;;
;;; JAR sez: Don't try to understand the implementation of this macro without
;;; proper supervision.

;;; Here are the desugaring rules used below
;;; 
;;; -------------------------------------------------------
;;; (match e (pat_1 e_1) ... (pat_n e_n))
;;; 
;;; -> expand(e,
;;;           pat_1, ..., pat_n
;;;           e_1, ... e_n)
;;; 
;;; -------------------------------------------------------
;;; expand(e,
;;;        pat_1, ..., pat_n
;;;        e_1, ... e_n)
;;; 
;;; 
;;; -> (error "Match -- no pattern matched"), n = 0
;;; 
;;; -> (let ((id e))                  ; id is fresh
;;;      expand-pattern(pat_1
;;;                     id
;;;                     e_1
;;;                     expand(id, 
;;;                            pat_2, ... pat_n
;;;                            e_2, ..., e_n)))  else
;;;
;;; -------------------------------------------------------
;;; 
;;; expand-pattern(pat, v, succ-exp, fail-exp)  ; Note v is always an id
;;; 
;;; -> succ-exp, pat = _
;;; 
;;; -> (if (=pat v) succ-exp fail-exp), pat a literal
;;;                                   ; Note this is *only* place where failure expression
;;;                                   ; can be evaluated, which makes sense, since
;;;                                   ; it's the only place that can really detect
;;;                                   ; a mismatch
;;; 
;;; -> (let ((pat v)) succ-exp), pat a variable
;;; 
;;; -> (e v (lambda (id_1 ... id_n) ; where id_i = pat_i, pat_i is a variable
;;;                                 ;              is fresh, otherwise
;;;           expand-sub-patterns(pat_1, ..., pat_n,
;;;                               id_1, ... id_n,
;;;                               succ-exp,
;;;                               fail-exp))
;;;         (lambda () ,fail-exp)), pat = (e pat_1 ... pat_n)
;;; 
;;; -------------------------------------------------------
;;; 
;;; expand-sub-patterns(pat_1, ..., pat_n,
;;;                     id_1, ... id_n,
;;;                     succ-exp,
;;;                     fail-exp)
;;; 
;;; -> succ-exp, n = 0
;;; 
;;; -> expand-pattern(pat_1
;;;                   id_1
;;;                   expand-sub-patterns(pat_2, ..., pat_n,
;;;                                       id_2, ..., id_n,
;;;                                       succ-exp,
;;;                                       fail-exp)
;;;                   (fail-exp))
;;; 
;;;-------------------------------------------------------------------
;;; Example (note extra error checking and space-saving code
;;;          optimizations not necessarily implied by above rules:
;;;
;;;  (match x
;;;    ((foo~ a (bar~ b)) (list a b))
;;;    (_ '()))) 
;;;
;;;  -- desugars to -->
;;;
;;; (let
;;;  ((#fail-254 (lambda () ())))  ; Why isn't the second () quoted here?
;;;  (foo~
;;;   x
;;;   (lambda
;;;    #success-arg-256
;;;    (if
;;;     (not (= (*mini-fx-length* #success-arg-256) 2))
;;;     (*mini-fx-success-number-of-args-mismatch* '(a (bar~ b)) 2)
;;;     (apply
;;;      (lambda
;;;       (a #temp-255)
;;;       (bar~
;;;        #temp-255
;;;        (lambda
;;;         #success-arg-257
;;;         (if
;;;          (not (= (*mini-fx-length* #success-arg-257) 1))
;;;          (*mini-fx-success-number-of-args-mismatch* '(b) 1)
;;;          (apply (lambda (b) (list a b)) #success-arg-257)))
;;;        #fail-254))
;;;      #success-arg-256)))
;;;   #fail-254))



;;; ---------------------------------------------------------------------------
;;; The MATCH macro 

(define-scheme+-macro (match thing . clauses)

  (define (expand thing clauses)
    (if (null? clauses)
	`(scheme+/runtime-error "MATCH -- no pattern matched to disc." ,thing)
	(let ((clause (car clauses)))
	  (define (make-matcher disc clauses)
	    `(scheme+/match-clauses ,disc
				    '(match ,thing ,@clauses)
				    ,@(map (expand-clause disc) clauses)))
	  (if (not (pair? thing))
	      ;; Optimization where discriminant is a literal or variable
	      (make-matcher thing clauses)
	      ;; Name discriminate so that only evaluate once
	      (let ((thing-gensym (scheme+/gensym 'thing)))
		`(LET ((,thing-gensym ,thing))
		   ,(make-matcher thing-gensym clauses)))))))


  (define (expand-clause thing)
    (lambda (clause)
      ;; should return a failure acceptor
      (let* ((failure (scheme+/gensym 'fail))
	     (make-failure-acceptor (lambda (body)
				      `(lambda (,failure) ,body))))
	(cond ((with-fail? clause)
	       (make-failure-acceptor (expand-with-fail thing clause failure)))
	      ((and (pair? clause)
		    (pair? (cdr clause))
		    (null? (cddr clause)))
	       (make-failure-acceptor
		(expand-pattern-top (car clause) thing (cadr clause) failure)))
	      (else (scheme+/syntax-error "Invalid match clause syntax: " clause))))))

;; Original version without user-level capture of the failure continuation
;   (define (expand-clause thing clause fail-exp)
;     (if (and (pair? clause)
; 	     (pair? (cdr clause))
; 	     (null? (cddr clause)))
; 	(expand-pattern (car clause) thing (cadr clause) fail-exp)
;         (scheme+/syntax-error "Invalid match clause syntax" clause)))

  ;; With-fail allows the user to capture the failure continuation.  Ie,
  ;; (match <foo>
  ;;   ((add e e) <body>))
  ;;
  ;; would be written:
  ;;
  ;; (match <foo>
  ;;   ((add e e.2)
  ;;    (with-fail
  ;;       (lambda (fail)
  ;;          (if (equal? e e.2)
  ;;              <body>
  ;;              (fail))))))
  ;;
  ;; Idea for new sugar on-top of with-fail (trevor)
  ;; (<pat> (where <exp_test> <exp_body))
  ;; ==> (<pat> (with-fail (lambda (fail) (if <exp_test> <exp_body> (fail)))))
  ;;
  (define (with-fail? clause)
    (and (pair? clause)
	 (pair? (cdr clause))
	 (null? (cddr clause))
	 (let ((body (cadr clause)))
	   (and (pair? body)
		(pair? (cdr body))
		(null? (cddr body))
		(eq? (car body) 'with-fail)))))

  (define (fail-body clause)
    (cadr (cadr clause)))

  (define (expand-with-fail thing clause fail-name)
    ;; Be sure to call expand-pattern-top to handle
    ;; duplicate pattern variables.
    (expand-pattern-top (car clause)
			thing
			`(scheme+/handle-with-fail ,(fail-body clause)
						   ,fail-name)
			fail-name))
  ;; End with-fail.


  ;; expand-pattern-top: entry point for pattern expansion
  ;;   First grovell pat to find duplicate pattern variables so we can
  ;;   use with-fail to make them do the right thing.  
  (define (expand-pattern-top pat thing succ-exp fail-name)
    (with-values (lambda () (rename-pattern-variables pat empty-dict))
      (lambda (new-pat dict)
	(let ((dict (list-transform-negative dict used-once?)))
	  (if (null? dict)
	      (expand-pattern pat thing succ-exp fail-name)
	      ;; Note: could just call expand-pattern directly as fail-name
	      ;; is in scope:
	      ;; (expand-pattern new-pat thing
	      ;;                 (if (and ,@(gen-check-dups dict))
	      ;;                     ,succ-exp
	      ;;                     (,fail-name))
	      ;;                 fail-name)
	      (expand-with-fail
	       thing
	       (let ((fail-gensym (scheme+/gensym 'fail)))
		 `(,new-pat (with-fail
			     (lambda (,fail-gensym)
			       (if (and ,@(gen-check-dups dict))
				   ,succ-exp
				   (,fail-gensym))))))
	       fail-name))))))

  (define (rename-pattern-variables pat dict)
    ;; Rename duplicate pattern variables and 
    ;; use values to return new pattern and a dictionary of pattern variables
    ;; Eg. (mul (add e e) (var x)) ==> (values (add e e.2) ((e e.2) (x)))
    (cond ((eq? pat '_) (values pat dict))
	  ((symbol? pat)
	   (if (in-dict? pat dict)
	       (let ((new-var (scheme+/gensym pat)))
		 (extend-entry! pat new-var dict)
		 (values new-var dict))
	       (values pat (add-entry pat dict))))
	  ((not (pair? pat)) (values pat dict))	;; constant pattern

	  ((or (eq? (car pat) 'symbol)
	       (eq? (car pat) 'quote))
	   (values pat dict))	   
	  ((eq? (car pat) 'quasiquote)
	   (with-values (lambda () (rename-in-quasiquote (cadr pat) dict))
	     (lambda (new-cadr dict)
	       (values `(,'quasiquote ,new-cadr) dict))))
	  (else
	   ;; (constructor pat1 pat2 ... patn)
	   (with-values (lambda () (rename-list (cdr pat) dict))
	     (lambda (patterns dict)
	       (values `(,(car pat) ,@patterns) dict))))
	  ))

  (define empty-dict '())
  (define (in-dict? x dict) (assoc x dict))
  (define (add-entry x dict) `((,x) ,@dict))
  (define (extend-entry! x new dict)
    (let ((entry (assoc x dict)))
      (set-cdr! entry (cons new (cdr entry)))
      ))
  (define (used-once? dict-entry)
    (= (length dict-entry) 1))

  (define (gen-check-dups dict)
    (if (null? dict)
	'()
	`(,(gen-ensure-equal (car dict)) ,@(gen-check-dups (cdr dict)))))
  (define (gen-ensure-equal lst)
    (let ((first (car lst)))
      `(and ,@(let loop ((rest (cdr lst)))
	       (if (null? rest)
		   '()
		   ;;; Lyn changed on 9/10/94 to install scheme+/equal?
		   ; `(((get-real equal?) ,first ,(car rest))
		   ;   ,@(loop (cdr rest)))
		   `((scheme+/equal? ,first ,(car rest))
		     ,@(loop (cdr rest))))))))
  
  (define (rename-list lst dict)
    (if (null? lst)
	(values '() dict)
	(with-values (lambda () (rename-pattern-variables (car lst) dict))
	  (lambda (new-head dict)
	    (with-values (lambda () (rename-list (cdr lst) dict))
	      (lambda (new-tail dict)
		(values (cons new-head new-tail) dict)))))))

  (define (rename-in-quasiquote body dict)
    (define (descend-quasi x level dict)
      (cond ((eq? x '_)			; Handle _ specially -- don't rename!
	     (values x dict)) 
	    ((symbol? x)		; Level 0 symbols get added to dict
	     (if (= level 0)
		 (if (in-dict? x dict)
		     (let ((tmp (scheme+/gensym x)))
		       (extend-entry! x tmp dict)
		       (values tmp dict))
		     (values x (add-entry x dict)))
		 (values x dict)))
	    ((not (pair? x))            ; Other atomic data (including empty list)
	     (values x dict))           ; are treated as constants.
	    ((and (not (null? (cdr x))) (null? (cddr x))) ; List of length 2
	     (case (car x)
	       ((unquote)
		(if (= level 0)
		    (scheme+/syntax-error "unquote too deep")
		    (with-values (lambda ()
				   (descend-quasi (cadr x) (- level 1) dict))
		      (lambda (new dict)
			(values `(,'unquote ,new) dict)))))
	       ((unquote-splicing)
		(if (= level 0)
		    (scheme+/syntax-error "unquote-splicing too deep")
		    (with-values (lambda ()
				   (descend-quasi (cadr x) (- level 1) dict))
		      (lambda (new dict)
			(values `(,'unquote-splicing ,new) dict)))))
	       ((quasiquote)
		(with-values (lambda ()
			       (descend-quasi (cadr x) (+ level 1) dict))
		  (lambda (new dict)
		    (values `(,'quasiquote ,new) dict))))
	       ((quote)
		(with-values (lambda ()
			       (descend-quasi (cadr x) level dict))
		  (lambda (new dict)
		    (values `(,'quote ,new) dict))))
	       (else
		(descend-quasi-deconstruction x level dict))))
	    (else (descend-quasi-deconstruction x level dict))))
    (define (descend-quasi-deconstruction lst level dict)
      ;; 
      ;; LST must be non-empty.
      ;; 
      ;; If LEVEL is 0, then LST is a deconstruction and the car of LST
      ;; is a constructor. The constructor should not be renamed, but
      ;; its arguments should be.
      ;; 
      ;; If LEVEL is not 0, treat LST as a regular list.
      ;;
      (if (= level 0)
	  (with-values (lambda () (descend-quasi-list (cdr lst) level dict))
	    (lambda (new-args dict)
	      (values (cons (car lst) ; The constructor 
			    new-args)
		      dict)))
	  (descend-quasi-list lst level dict)))
    (define (descend-quasi-list x level dict)
      (if (null? x)
	  (values '() dict)
	  (with-values (lambda () (descend-quasi (car x) level dict))
	    (lambda (first dict)
	      ;BJR: change to descend-quasi to handle dotted pairs
	      ;(with-values (lambda() (descend-quasi-list (cdr x) level dict)))
	      (with-values (lambda () (descend-quasi (cdr x) level dict))
		(lambda (rest dict)
		  (values (cons first rest) dict)))))))
    ; (trace-entry descend-quasi)
    ; (trace-entry descend-quasi-list)
    ; (trace-entry descend-quasi-deconstruction)
    (descend-quasi body 1 dict)
    )
  ;; End rename-pattern-variables


  ; succ-exp is an expressions
  ; fail-name is a symbol -- name of the current failure continuation
  (define (expand-pattern pat thing succ-exp fail-name)
    (let ((fail-exp `(,fail-name)))
      (cond ((eq? pat '_) succ-exp)
	    ((symbol? pat)
	     (if (eq? pat thing) 
		 succ-exp	  ;Optimization for same variable that works
				  ;in conjunction with optimization for naming
				  ;success lambda args below
		 `(let ((,pat ,thing)) ,succ-exp)))

	    ;; Don't make any assumptions about thing.
; 	    ((number? pat)
; 	     `(if ((get-real =) ,thing ,pat) ,succ-exp ,fail-exp))
; 	    ((boolean? pat)
; 	     `(if ((get-real eq?) ,thing ,pat) ,succ-exp ,fail-exp))
; 	    ((char? pat)
; 	     `(if ((get-real char=?) ,thing ,pat) ,succ-exp ,fail-exp))
; 	    ((string? pat) 
; 	     `(if ((get-real string=?) ,thing ,pat) ,succ-exp ,fail-exp))

	    ((number? pat)
	     `(if ((get-real equal?) ,thing ,pat) ,succ-exp ,fail-exp))
	    ((boolean? pat)
	     `(if ((get-real equal?) ,thing ,pat) ,succ-exp ,fail-exp))
	    ((char? pat)
	     `(if ((get-real equal?) ,thing ,pat) ,succ-exp ,fail-exp))
	    ((string? pat) 
	     `(if ((get-real equal?) ,thing ,pat) ,succ-exp ,fail-exp))

	    ((not (pair? pat))
	     (scheme+/syntax-error "unrecognized MATCH pattern: " pat))
	    ((eq? (car pat) 'symbol)
	     `(if ((get-real eq?) ,thing ,pat) ,succ-exp ,fail-exp))
	    ((eq? (car pat) 'quote)
	     (let ((pred (if (symbol? (cadr pat))
			     '(get-real eq?) ;Optimization
			     '(get-real equal?))))
	       `(if (,pred ,thing ,pat) ,succ-exp ,fail-exp)))
	    ((eq? (car pat) 'quasiquote)
	     (expand-pattern (expand-quasiquote (cadr pat) 0)
			     thing succ-exp fail-name))
	    (else
	     (expand-compound-pattern pat thing succ-exp fail-name)))))

  ; Expand a pattern of the form (op arg ...).
  ; op is assumed to be an injection or construction procedure.
  ; If it's not, you'll get a Scheme error of the form
  ; "unbound variable ~MATCH-OP".

  (define (expand-compound-pattern pat thing succ-exp fail-name)
    (let ((number-of-sub-patterns (length (cdr pat)))
	  (success-arg-gensym (scheme+/gensym 'success-arg))
	  (names (map (lambda (pat)
			(if (and (symbol? pat) (not (eq? pat '_)))
			    pat		;Optimization that works in conjunction
					;with variable case for EXPAND-PATTERN 
			    (scheme+/gensym 'temp)))
		      (cdr pat)))
	  )
      `(
	scheme+/deconstruct-carefully
	,(car pat)
	',(cdr pat)
	,number-of-sub-patterns
	,thing
	(LAMBDA ,names
	  ,(let expand-sub-patterns ((pats (cdr pat))
				     (names names))
	     (if (null? pats)
		 succ-exp
		 (expand-pattern (car pats)
				 (car names)
				 (expand-sub-patterns (cdr pats) (cdr names))
				 fail-name))))
	,fail-name)))


  ; This is JAR's quasiquote pattern handler, which is exceptionally clever.
  ; It tries to avoid the desugaring into CONS~ 
  ; unless it absolutely has to.  Here are some examples:
  ; 
  ; `(a b) -> '(a b)
  ; `(,a b) -> (list->sexp~ (cons~ a '(b)))
  ; `(a ,b) -> (list->sexp~ (cons~ 'a (cons~ b '())))
  ; `(,a ,b) -> (list->sexp~ (cons~ a (cons~ b '())))
  ; `(,a ,@b) -> (list->sexp~ (cons~ a b))


  ;;----------------------------------------------------------------------
  ;; LYN'S NOTES ON QUOTATION 
  ;
  ; What I had in mind is expressed by the following rewrite rules 
  ; 
  ;   (quasiquote (unquote ?a)) => ?a
  ; 
  ;   (quasiquote ((unquote-splicing ?a) ?extra ?rest ...) => Error! illegal ,@
  ; 
  ;   (quasiquote ((unquote-splicing ?a)) => ?a
  ; 
  ;   (quasiquote (unquote-splicing ?a)) => Error! illegal ,@
  ; 
  ;   (quasiquote (?a . ?b)) => (cons (quasiquote ?a) (quasiquote ?b))
  ; 
  ;   (quasiquote ?a) => (quote ?a)
  ; 
  ; (These rules must be applied in the order shown to get the precedence 
  ; right.)
  ; 
  ; These rules treat UNQUOTE and UNQUOTE-SPLICING specially in the 
  ; context of a quasiquote.  Granted, this can give some weird effects;
  ; the following are transcripts of the pattern matching implementation of
  ; Scheme+:
  ; 
  ; ;;;--------------------------------------------------
  ;    (define x 17)
  ;    (define y '(23))
  ; 
  ;    (match '(1 . 2)
  ;      (`(,x . ,y) (list y x)))
  ;    ;Value 56: (2 1)
  ; 
  ;    (match '(1 . 2)
  ;      (`(,x unquote y) (list y x)))
  ;    ;Value 57: (2 1)
  ; 
  ;    (match '(1 . 2)
  ;      (`(,x unquote y) (list y x)))
  ;    ;Value 57: (2 1)
  ; 
  ;    (match '(1 unquote y)
  ;      (`(,x unquote y) (list y x)))
  ;    ;Value 58: (,y 1)
  ; 
  ;    (match '(1 . 2)
  ;      (`(,x unquote y z) (list y x)))
  ;    ;No pattern matched!
  ;    ;To continue, call RESTART with an option number:
  ; 
  ;    (match '(1 unquote y z)
  ;      (`(,x unquote y z) (list y x)))
  ;    ;Value 64: ((23) 1)  ; The (23) comes from definition of Y at top.
  ; 
  ;    (match '(1 2)
  ;      (`(,x ,@y) (list y x)))
  ;    ;Value 75: ((2) 1)
  ; 
  ;    (match '(1 2)
  ;      (`(,x . ,@y) (list y x)))
  ;    ;Illegal use of ,@ in pattern
  ;    ;To continue, call RESTART with an option number:
  ;    ; (RESTART 1) => Return to read-eval-print level 1.
  ; 
  ;    (match '(1 2)
  ;      (`(,x unquote-splicing y) (list y x)))
  ;    ;Illegal use of ,@ in pattern
  ;    ;To continue, call RESTART with an option number:
  ; 
  ;    (match '(1 unquote-splicing y z)
  ;      (`(,x unquote-splicing y z) (list y x)))
  ;    ;Value 76: ((23) 1)  ; The (23) comes from definition of Y at top.
  ; 
  ; 
  ; ;;;--------------------------------------------------
  ; 
  ; The quirkiness is exhibited only with the symbols UNQUOTE and
  ; UNQUOTE-SPLICING in a QUASIQUOTE context; all other symbols behave as
  ; expected. This is OK (IMHO) because these are part of the quasiquote
  ; language.  Even more convincing, the above interpretation is consistent 
  ; with what Scheme (at least MIT Scheme) does with QUASIQUOTE in a
  ; non-pattern context. Observe:
  ; 
  ;    ;; Assume X is 17 and Y is (23)
  ; 
  ;    `(,x . ,y)
  ;    ;Value 81: (17 23)
  ; 
  ;    `(,x unquote y)
  ;    ;Value 78: (17 23)
  ; 
  ;    `(,x unquote y z)
  ;    ;Value 77: (17 unquote y z)
  ; 
  ;    `(,x ,@y)
  ;    ;Value 79: (17 23)
  ; 
  ;    `(,x . ,@y)
  ;    ;Syntax error: ,@ in illegal context: y
  ;    ;To continue, call RESTART with an option number:
  ; 
  ;    `(,x unquote-splicing y)
  ;    ;Syntax error: ,@ in illegal context: y
  ;    ;To continue, call RESTART with an option number:
  ; 
  ;    `(,x unquote-splicing y z)
  ;    ;Value 80: (17 unquote-splicing y z)
  ; 
  ; 
  ; The only inconsistent usage is the treatment of ,@ in a pattern context
  ; to avoid backtracking.
  ; 
  ;;----------------------------------------------------------------

  ;; NOTE: these procedures are all written with names using 'deconstructor' 
  ;; which is an out-of-date term -- the correct terminology is 'unbunlder'.
  
  (define (expand-quasiquote x level)
    (descend-quasiquote x level finalize-quasiquote))

  (define (descend-quasiquote x level return)
    (cond ((not (pair? x))		; Includes null?
	   (return 'quote x))
	  ((and (not (null? (cdr x))) (null? (cddr x))) ; List of length 2
	   (case (car x)
	     ((unquote)
	      (if (= level 0)
		  (return 'unquote (cadr x))
		  (descend-interesting x (- level 1) unquoted->sexp return)))
	     ((unquote-splicing)
	      (if (= level 0)
		  (return 'unquote-splicing (cadr x))
		  (descend-interesting x (- level 1) unquoted-splicing->sexp return)))
	     ((quasiquote)
	      (descend-interesting x (+ level 1) quasiquoted->sexp return))
	     ((quote)
	      (descend-interesting x level quoted->sexp return))
	     (else
	      (descend-quasiquote-list x level return))))
	  (else (descend-quasiquote-list x level return))))

  (define (descend-interesting x level inject return)
    (descend-quasiquote (cadr x) level
      (lambda (mode arg)
	(if (eq? mode 'quote)
	    (return 'quote x)
	    (return 'unquote `(,inject ,(finalize-quasiquote mode arg)))))))

  (define (descend-quasiquote-list x level return)
    (descend-quasiquote-tail x level
      (lambda (mode arg)
	(if (eq? mode 'quote)
	    (return 'quote x)
	    ;;BJR: list-sexp is identity and only needed for typing concerns
	    ;; (return 'unquote `(,list->sexp ,arg))
	    (return 'unquote arg)))))

; Modified below to handle dotted pairs.
;   (define (descend-quasiquote-tail x level return)
;     (if (null? x)
; 	(return 'quote x)
; 	(descend-quasiquote-tail (cdr x) level
; 	  (lambda (cdr-mode cdr-arg)
; 	    (descend-quasiquote (car x) level
; 	      (lambda (car-mode car-arg)
; 		(cond ((and (eq? car-mode 'quote) (eq? cdr-mode 'quote))
; 		       (return 'quote x))
; 		      ((eq? car-mode 'unquote-splicing)
; 		       (cond ((and (eq? cdr-mode 'quote) (null? cdr-arg))
; 			      ;; (,@mumble)
; 			      (return 'unquote car-arg))  ;Type must be a list!
; 			     (else (scheme+/syntax-error "Illegal use of @ in a quasiquoted pattern."))))
;                            ; JAR allowed the following, but doesn't make sense
; 			   ; without backtracking.
; 			   ; (else
; 			   ;  ;; (,@mumble ...)
; 			   ;  (return 'unquote
; 			   ;          `(,append-word ,car-arg
; 			   ;  		             ,(finalize-quasiquote
; 			   ;                           cdr-mode cdr-arg))))
; 		      (else
; 		       (return 'unquote
; 			       ;; what is cons-word supposed to be?
; 			       `(,cons ,(finalize-quasiquote car-mode car-arg)
; 					    ,(finalize-quasiquote cdr-mode cdr-arg)))))))))))

  (define (descend-quasiquote-tail x level return)
    (if (null? x)
	(return 'quote x)
	;; BJR&LYN: This handles dotted-pair too!
	(descend-quasiquote (cdr x) level
	  (lambda (cdr-mode cdr-arg)
	    (descend-quasiquote (car x) level
	      (lambda (car-mode car-arg)
		(cond ((and (eq? car-mode 'quote) (eq? cdr-mode 'quote))
		       (return 'quote x))
		      ((eq? car-mode 'unquote-splicing)
		       (cond ((and (eq? cdr-mode 'quote) (null? cdr-arg))
			      ;; (,@mumble)
			      (return 'unquote car-arg)) ;Type must be a list!
			     (else (scheme+/syntax-error "Illegal use of @ in a quasiquoted pattern."))))
		      ((and (eq? cdr-mode 'unquote-splicing)
			    (not (pair? (cdr x))))
		       ;; (foo . ,@x)
		       (scheme+/syntax-error 
			"Illegal use of @ in a quasiquoted pattern."))
		      ; JAR allowed the following, but doesn't make sense
		      ; without backtracking.
		      ; (else
		      ;  ;; (,@mumble ...)
		      ;  (return 'unquote
		      ;          `(,append-word ,car-arg
		      ;  		             ,(finalize-quasiquote
		      ;                           cdr-mode cdr-arg))))
		      (else
		       (return 'unquote
			       `(,cons ,(finalize-quasiquote car-mode car-arg)
				       ,(finalize-quasiquote cdr-mode cdr-arg)))))))))))


  (define (finalize-quasiquote mode arg)
    (case mode
      ((quote) `',arg)
      ((unquote) arg)
      ((unquote-splicing)
       (scheme+/syntax-error ",@ in illegal context: " arg)) ;`,@x or ``,,@x or `(y . ,@x)
      (else
       (scheme+/syntax-error "quasiquote bug: " (list mode arg)))))

  (define (non-atomic-deconstructors clauses)
    (remove-duplicates
     (mapcan (lambda (c)
	       (filter
		pair?
		(deconstructors (match-clause-pattern c))))
	     clauses)))

  (define (deconstructors pat)
    (cond ((not (pair? pat)) '())
	  ((quote? pat) '())
	  ((quasiquote? pat)
	   (mapcan deconstructors
		   (quasiquote-embedded-expressions (quasiquote-text pat))))
	  (else 
	   (cons (car pat)
		 (mapcan deconstructors (cdr pat))))))

  (define (filter pred lst)
    (cond ((null? lst) '())
	  ((pred (car lst))
	   (cons (car lst) (filter pred (cdr lst))))
	  (else (filter pred (cdr lst)))))

  (define (mapcan proc lst)
    (if (null? lst) 
	'()
	(append (proc (car lst))
		(mapcan proc (cdr lst)))))

  (define (remove-duplicates lst)
    (if (null? lst)
	'()
	(let ((result (remove-duplicates (cdr lst))))
	  (if (member (car lst) result)
	      result
	      (cons (car lst) result)))))

  (define (quasiquote-embedded-expressions exp)
    (let descend-quasiquote ((exp exp)
			     (level 0))
      (cond ((not (pair? exp)) '())
	    ((quote? exp) '())
	    ((quasiquote? exp) 
	     (descend-quasiquote (quasiquote-text exp) (+ level 1)))
	    ((unquote? exp) 
	     (if (= level 0) 
		 (list (unquote-text exp))
		 (descend-quasiquote (unquote-text exp) (- level 1))))
	    ((unquote-splicing? exp)
	     (if (= level 0) 
		 (list (unquote-splicing-text exp))
		 (descend-quasiquote (unquote-splicing-text exp) (- level 1))))
	    (else (mapcan (lambda (e) (descend-quasiquote e level)) exp))
	    )))

  (define (clauses-deconstructor-subst clauses subst)
    (map (lambda (clause) 
	   (cons (pattern-deconstructor-subst (car clause) subst)
		 (cdr clause)))
	 clauses))

  (define (pattern-deconstructor-subst pat subst)
    (cond ((not (pair? pat)) pat)
	  ((quote? pat) pat)
	  ((quasiquote? pat) 
	   (quasiquote-deconstructor-subst (quasiquote-text pat) subst))
	  (else (cons (subst (car pat))
		      (map (lambda (p)
			     (pattern-deconstructor-subst p subst))
			   (cdr pat))))))

  (define (quasiquote-deconstructor-subst pat subst)
    (let descend-quasiquote ((exp pat)
			     (level 0))
      (cond ((not (pair? exp)) exp)
	    ((quote? exp) exp)
	    ((quasiquote? exp) 
	     (quasiquote-make
	      (descend-quasiquote (quasiquote-text exp) (+ level 1))))
	    ((unquote? exp) 
	     (unquote-make
	      (if (= level 0)
		  (pattern-deconstructor-subst
		   (unquote-text exp) 
		   subst)
		  (descend-quasiquote (unquote-text exp) (- level 1)))))
	    ((unquote-splicing? exp)
	     (unquote-splicing-make
	      (if (= level 0)
		  (pattern-deconstructor-subst
		   (unquote-splicing-text exp) 
		   subst)
		  (descend-quasiquote (unquote-splicing-text exp) (- level 1)))))
	    (else (map (lambda (e) (descend-quasiquote e level)) ex)))))

  (define match-clause-pattern first)

  (define (predicate sym)
    (lambda (exp)
      (if (pair? exp)
	  (eq? (car exp) sym)
	  #f)))

  ; Quoting
  (define quote? (predicate 'quote))
  (define (quote-make text) (list 'quote text))
  (define quote-text second)
  (define quasiquote? (predicate 'quasiquote))
  (define (quasiquote-make text) (list 'quasiquote text))
  (define quasiquote-text second)
  (define unquote? (predicate 'unquote))
  (define (unquote-make text) (list 'unquote text))
  (define unquote-text second)
  (define unquote-splicing? (predicate 'unquote-splicing))
  (define (unquote-splicing-make text) (list 'unquote-splicing text))
  (define unquote-splicing-text second)


  ;; Main body of the MATCH macro
  (expand thing clauses)

  ) ; End MATCH macro expander




(define (scheme+/match-clauses disc match-exp . rest)
  (let loop ((clauses rest))
    (if (null? clauses)
	(scheme+/runtime-error "MATCH -- no pattern matched to disc." 
			       disc match-exp)
	((car clauses)
	 (lambda () 
	   (loop (cdr clauses)))))))

;; Procedure for doing deconstruction out-of-line
(define (scheme+/deconstruct-carefully
	 constr sub-patterns nargs thing succ-cont fail-cont)
  ((unbundler constr)
   thing
   (lambda success-args
     (if (not (= (length success-args) (length sub-patterns)))
	 (scheme+/success-number-of-args-mismatch sub-patterns
						  success-args)
	 (apply succ-cont success-args)))
   fail-cont))


(define (scheme+/handle-with-fail fail-acceptor fail-cont)
  (let ((token (list 'fail-token)))
    ;; fail-acceptor is the body of the with-fail
    ;; fail-cont is the failure continuation for this clause
    ;;OLD: fail-body is a failure acceptor, so just apply it to the 
    ;;     current failure: (fail-acceptor fail-cont)
    ;;BJR: need to really abort clause so grab continuation
    (let ((return-value
	   (call-with-current-continuation
	    (lambda (k)
	      (fail-acceptor
	       ;; abort by returning failure continuation
	       (lambda () (k token)))))))
      (if (eq? return-value token)
	  ;; we're aborting
	  (fail-cont)
	  return-value))))

;;; MATCH End
;;; $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$

;;; $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$
;;; SCHEME+/GENSYM

(define scheme+/gensym
  (let ((counter 0))
    (lambda (sym)
      (let ((result (string->symbol
		     ;; Symbols beginning with # aren't recognized by reader, 
		     ;; so this can't conflict with a user-specified name.
		     (string-append "#" 
				    (symbol->string sym) 
				    "-" 
				    (number->string counter)))))
	(set! counter (+ counter 1))
	result))))
;;; $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$



(define (scheme+/success-number-of-args-mismatch patterns exps)
  (scheme+/runtime-error 
   (string-append "the number of sub-patterns in a match clause ("
		  (number->string (length patterns))
		  ") is not equal to \nthe number of fields in a deconstructed datatype object ("
		  (number->string (length exps))
		  "). \nTried to match the following patterns and expressions:\n")
   (list 'patterns: patterns 'exps: exps)))
 
;;---------------------------------------------------
;; top-level.scm
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; Scheme+ Top-Level
;;;
;;; Author:        Lyn                     
;;; Creation Date: 6/23/94
;;; Log:
;;; * 9/18/04 (amdragon): Modified to work without syntax tables and
;;;    to generate the interactive environment differently for Scheme
;;;    7.7
;;; * 8/19/94 (reistad): Reworked to use low level macros.
;;; * 8/11/94 (lyn): Adapted Mini-FX SF, CF, and path stuff to Scheme+
;;;   Also added an explicit Scheme+ interactive environment.
;;;
;;; Documentation: 
;;; * Controls the integration of Scheme+ within MIT Scheme.  
;;; * Based on the Mini-FX top level.
;;; * Ignores the deep issues of interactions between syntactic definitions
;;;   and first-class environments. (According to CPH, this is why 
;;;   SYNTAX-RULES aren't standard in MIT Scheme.)
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;; History 
;;;
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;----------------------------------------------------------------------------
;;; TOP-LEVEL EVALUATION

;;; Loading files in Scheme+ requires special handling of unsyntaxed files.

(define (scheme+/load filename)
  (load filename scheme+/interactive-environment))

(define (scheme+/eval exp env)
  (eval (syntax exp scheme+/interactive-environment) env))

;; Currently unsupported
;
(define (scheme+/sf filename . rest)
  ;; (sf/set-file-syntax-table! (->pathname filename) scheme+/syntax-table)
  ;; (apply sf filename rest)
  (error "SF for Scheme+ files is currently unsupported")
  )

(define (scheme+/cf filename . rest)
  ;; (sf/set-file-syntax-table! (->pathname filename) scheme+/syntax-table)
  ;; (apply cf filename rest)
  (error "CF for Scheme+ files is currently unsupported")
  )


;;;----------------------------------------------------------------------------
;;; Installation of evaluator

(define (scheme+/enter-top-level)

  ;; Change the emacs interface 
  ;; Doesn't seem to work.
  ;; (scheme-runtime/install-mfx-emacs-interface!)

  ;; No longer needed -- reistad 8/19/94
  ;  (scheme-runtime/install-evaluator! (lambda (sexp env st)
  ;				       (scheme+/eval sexp env)))

  (scheme-runtime/install-environment! scheme+/interactive-environment)
  (display (string-append 
	    "\nYou are now typing at the Scheme+ interpreter (version "
	    scheme+/version
	   ")"
	   ))
  )


(define (scheme+/exit-top-level)

  ;; No longer needed -- reistad 8/19/94
  ;  (scheme-runtime/install-default-evaluator!)
  (scheme-runtime/install-environment! user-initial-environment)
  (display "\nYou are now typing at the Scheme interpreter.")

  )

;; A new version of EQUAL? that provides equality checking of datatype instances
;;
(define (scheme+/equal? obj1 obj2)
  (cond ((eq? obj1 obj2) #t)
	((pair? obj1)
	 (if (pair? obj2)
	     (and (scheme+/equal? (car obj1) (car obj2))
		  (scheme+/equal? (cdr obj1) (cdr obj2)))
	     #f))
	((datatype-instance? obj1) 
	 (if (datatype-instance? obj2)
	     (scheme+/datatype-instance-equal? obj1 obj2)
	     #f))
	((number? obj1) 
	 (if (number? obj2)
	     (= obj1 obj2)
	     #f))
	((string? obj1) 
	 (if (string? obj2)
	     (string=? obj1 obj2)
	     #f))
	((char? obj1) 
	 (if (char? obj2)
	     (char=? obj1 obj2)
	     #f))
	((vector? obj1)
	 (if (vector obj2)
	     (and (= (vector-length obj1) (vector-length obj2))
		  (let loop ((i (- (vector-length obj1) 1)))
		    (if (< i 0)
			#t
			(and (scheme+/equal? (vector-ref obj1 i)
					     (vector-ref obj2 i))
			     (loop (- i 1))))))
	     #f))
	(else #f)
	))

(define (scheme+/datatype-instance-equal? dinst1 dinst2)
	 (and (eq? (datatype-instance-descriptor dinst1)
		   (datatype-instance-descriptor dinst2))
	      (eq? (datatype-instance-constructor dinst1)
		   (datatype-instance-constructor dinst2))
	      (scheme+/equal? (datatype-instance-args dinst1)
			      (datatype-instance-args dinst2))))

;; Handy synonyms

(define (scheme+) (scheme+/enter-top-level))
(define (scheme) 
  (display "\nYou are already typing at the Scheme interpreter."))

(define (scheme+/pp . args)
  ;; Version of PP that prints everything as code (nothing as tables).
  (if (version-7.1.3?)
      (apply pp args)
      (fluid-let ((*pp-lists-as-tables?* #f))
	(apply pp args))))

;;;----------------------------------------------------------------------------
;;; The Scheme+ Environment
;;; 
;;; This creates a new environment on top of user-initial-environment
;;; that contains a few Scheme+ specific bindings.

(define scheme+/interactive-environment
  (let ()
    (define load scheme+/load)
    (define eval scheme+/eval)
    (define sf scheme+/sf)
    (define cf scheme+/cf)
    (define equal? scheme+/equal?)
    (define user-initial-environment 'later)
    (define global-eval 
      (lambda (exp) (eval exp scheme+/interactive-environment)))
    (define scheme scheme+/exit-top-level)
    (define scheme+
      (lambda () 
	(display "\nYou are already typing at the Scheme+ interpreter.")))
    (define pp scheme+/pp)
    (procedure-environment (lambda (x) x)))
  )

(eval `(set! user-initial-environment ,scheme+/interactive-environment)
      scheme+/interactive-environment)


;;;----------------------------------------------------------------------------
;;; Version Handling

;;; Scheme version stuff

; (define (runtime-system)
;   (let ((answer '*))
;     (begin 
;       (for-each-system!
;        (lambda (sys)
; 	 (if (string=? (system/name sys)
; 		       "Runtime")
; 	     (set! answer (list (system/version sys)
; 				(system/modification sys))))))
;       (if (eq? answer '*)
; 	  (error "SCHEME+ INITIALIZATION ERROR: CAN'T FIND SCHEME RUNTIME VERSION")
; 	  answer))))

(define (runtime-system)
  (if (environment-bound? system-global-environment 'get-subsystem-version)
      (get-subsystem-version "Runtime")
      (let ((answer '*))
	(begin 
	  (for-each-system!
	   (lambda (sys)
	     (if (string=? (system/name sys)
			   "Runtime")
		 (set! answer (list (system/version sys)
				    (system/modification sys))))))
	  (if (eq? answer '*)
	      (error "SCHEME+ INITIALIZATION ERROR: CAN'T FIND SCHEME RUNTIME VERSION")
	      answer)))))

	   
(define runtime-version first)
(define runtime-modification second)

(define (version-7.1.3?) 
  (equal? (runtime-system) '(14 104)))

(define (version-7.2?) 
;; This is a hack. This also returns #t for 7.3 systems.
;; To discriminate for 7.3, explicitly test for it first!
;   (or (equal? (runtime-system) '(14 155))
;       (equal? (runtime-system) '(14 156))
;       (equal? (runtime-system) '(14 157)))
  (and (= (first (runtime-system)) 14)
       (>= (second (runtime-system)) 155)))

(define (version-7.3?)
  (and (= (first (runtime-system)) 14)
       (>= (second (runtime-system)) 166)))

;; amdragon 9/18/04 - Added support for Scheme 7.7 (mostly the same as
;; 7.3)
(define (version-7.7?)
  (and (= (first (runtime-system)) 15)
       (>= (second (runtime-system)) 3)))

(define (version-error)
  (error 
   (string-append 
    "SCHEME-+ VERSION ERROR\nScheme+ doesn't know how to handle runtime "
    (number->string (runtime-version (runtime-system)))
    "."
    (number->string (runtime-modification (runtime-system)))
    "\nContact 6821@psrg.lcs.mit.edu for help.")))

(define scheme-runtime/version-7.1.3/install-evaluator!
  (let ((repl-env (->environment '(runtime rep))))
    (lambda (evaluator) ; evaluator is SEXP x ENV x ST -> 
      (set! (access hook/repl-eval repl-env)
	    ;; Version 7.1.3 evaluators take an extra first arg
	    ;; that is ignored.
	    (lambda (repl sexp env st)
	      (evaluator sexp env st))))))

(define scheme-runtime/version-7.2/install-evaluator!
  (let ((repl-env (->environment '(runtime rep))))
    (lambda (evaluator) ; evaluator is SEXP x ENV x ST -> 
      (set! (access hook/repl-eval repl-env)
	    evaluator))))

(define scheme-runtime/install-evaluator!
  (cond 
   ((version-7.3?) scheme-runtime/version-7.1.3/install-evaluator!) ; kludge!
   ((version-7.1.3?) scheme-runtime/version-7.1.3/install-evaluator!)
   ((version-7.2?) scheme-runtime/version-7.2/install-evaluator!)
   ((version-7.7?) scheme-runtime/version-7.2/install-evaluator!)
   (else (version-error))))

;;; This is written to be independent of version
(define scheme-runtime/install-default-evaluator!
  (eval '(lambda () (set! hook/repl-eval default/repl-eval))
	(->environment '(runtime rep))))


;;; Patch for 7.1.3
(define (scheme-runtime/map proc . lsts)
  (define (map proc . lsts)
    (cond ((null? lsts) '())
	  ((null? (car lsts)) '())
	  ((null? (cdr lsts));; only one list to map over
	   (cons (proc (caar lsts))
		 (map proc (cdar lsts))))
	  (else
	   (cons (apply proc (map car lsts))
		 (apply map proc (map cdr lsts))))))
  (apply map proc lsts))

(if (version-7.1.3?)
    (set! map scheme-runtime/map)
    'nop)


;;; Environments

(define scheme-runtime/version-7.1.3/install-environment! 
  (eval '(lambda (env)
	   (let ((repl (nearest-repl))
		 (environment (->environment env)))
	     (set-repl-state/environment! (cmdl/state repl) environment)
	     (if (not (cmdl/parent repl))
		 (set! user-repl-environment environment))))
	(->environment '(runtime rep))))

(define scheme-runtime/version-7.2/install-environment! ge)

(define scheme-runtime/install-environment!
  (cond 
   ((version-7.1.3?) scheme-runtime/version-7.1.3/install-environment!)
   ((version-7.2?) scheme-runtime/version-7.2/install-environment!)
   ((version-7.7?) scheme-runtime/version-7.2/install-environment!)
   (else (version-error))))

(define (scheme-runtime/fasload-file? pathname)
  (let* ((port (open-input-file pathname))
	 (fasl-marker (peek-char port))
	 (result (and (not (eof-object? fasl-marker))
		      (= 250 (char->ascii fasl-marker)))))
    (begin
      (close-input-port port)
      result)))


;;; Dump/load

(define scheme-runtime/fasdump fasdump)
(define scheme-runtime/fasload fasload)


;;; Path stuff

(define scheme+/scheme+-ext "scm")
(define scheme+/bin-ext "bin")
(define scheme+/com-ext "com")

;; Get pathname of existing file ending in ".scm"
(define scheme-runtime/version-7.1.3/scheme+-pathname 
  (eval `(lambda (filename)
	   (find-true-pathname (->pathname filename) 
			       '(,scheme+/scheme+-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.2/scheme+-pathname 
  (eval `(lambda (filename)
	   (find-pathname filename '(,scheme+/scheme+-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.3/scheme+-pathname 
  (eval `(lambda (filename)
	   (with-values (lambda () 
			  (find-pathname filename 
					 '((,scheme+/scheme+-ext
					    ,',load/internal))))
	     (lambda (pathname loader) pathname)))
	(->environment '(runtime load))))


(define scheme-runtime/scheme+-pathname 
  (cond
   ((version-7.7?) scheme-runtime/version-7.3/scheme+-pathname)
   ((version-7.3?) scheme-runtime/version-7.3/scheme+-pathname)
   ((version-7.2?) scheme-runtime/version-7.2/scheme+-pathname)
   ((version-7.1.3?) scheme-runtime/version-7.1.3/scheme+-pathname)
   (else (version-error))))

;; Get pathname of existing file ending in either ".bin" or ".scm"
(define scheme-runtime/version-7.1.3/bin-pathname 
  (eval `(lambda (filename)
	   (find-true-pathname (->pathname filename) 
			       '(,scheme+/bin-ext
				 ,scheme+/scheme+-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.2/bin-pathname 
  (eval `(lambda (filename)
	   (find-pathname filename '(,scheme+/bin-ext
				     ,scheme+/scheme+-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.3/bin-pathname 
  (eval `(lambda (filename)
	   (with-values (lambda () 
			  (find-pathname filename 
					 '((,scheme+/scheme+-ext
					    ,',load/internal)
					   (,scheme+/bin-ext
					    ,',load/internal))))
	     (lambda (pathname loader) pathname)))
	(->environment '(runtime load))))

(define scheme-runtime/bin-pathname 
  (cond
   ((version-7.7?) scheme-runtime/version-7.3/bin-pathname)
   ((version-7.3?) scheme-runtime/version-7.3/bin-pathname)
   ((version-7.2?) scheme-runtime/version-7.2/bin-pathname)
   ((Version-7.1.3?) scheme-runtime/version-7.1.3/bin-pathname)
   (else (version-error))))

;; Get pathname for existing file ending in ".scm" and return a pathname
;; for the same file, but ending in ".bin"
(define scheme-runtime/version-7.1.3/sf-name
  (eval `(lambda (filename)
	   (pathname->string
	    (pathname-new-type
	     (find-true-pathname (->pathname filename) 
				 '(,scheme+/scheme+-ext))
	     ,scheme+/bin-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.2/sf-name
  (eval `(lambda (filename)
	   (->namestring
	    (pathname-new-type
	     (find-pathname filename 
			    '(,scheme+/scheme+-ext))
	     ,scheme+/bin-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.3/sf-name
  (eval `(lambda (filename)
	   (->namestring
	    (pathname-new-type
	     (with-values (lambda () 
			    (find-pathname filename 
					   '((,scheme+/scheme+-ext
					      ,',load/internal))))
	       (lambda (pathname loader) pathname))
	     "bin")))
	(->environment '(runtime load))))


(define scheme-runtime/sf-name 
  (cond
   ((version-7.7?) scheme-runtime/version-7.3/sf-name)
   ((version-7.3?) scheme-runtime/version-7.3/sf-name)
   ((version-7.2?) scheme-runtime/version-7.2/sf-name)
   ((version-7.1.3?) scheme-runtime/version-7.1.3/sf-name)
   (else (version-error))))


;; Get pathname for existing file ending in ".scm" or ".bin" and return a  
;; pathname for the same file, but ending in ".com"
(define scheme-runtime/version-7.1.3/cf-name
  (eval `(lambda (filename)
	   (pathname->string
	    (pathname-new-type
	     (find-true-pathname (->pathname filename) 
				 '(,scheme+/scheme+-ext
				   ,scheme+/bin-ext))
	     ,scheme+/com-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.2/cf-name
  (eval `(lambda (filename)
	   (->namestring
	    (pathname-new-type
	     (find-pathname filename '(,scheme+/scheme+-ext
				       ,scheme+/bin-ext))
	     ,scheme+/com-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/version-7.3/cf-name
  (eval `(lambda (filename)
	   (->namestring
	    (pathname-new-type
	     (with-values (lambda () 
			    (find-pathname filename 
					   '((,scheme+/scheme+-ext
					      ,',load/internal)
					     (,scheme+/bin-ext
					      ,',load/internal))))
	       (lambda (pathname loader) pathname))
	     ,scheme+/com-ext)))
	(->environment '(runtime load))))

(define scheme-runtime/cf-name 
  (cond
   ((version-7.7?) scheme-runtime/version-7.3/cf-name)
   ((version-7.3?) scheme-runtime/version-7.3/cf-name)
   ((version-7.2?) scheme-runtime/version-7.2/cf-name)
   ((version-7.1.3?) scheme-runtime/version-7.1.3/cf-name)
   (else (version-error))))


;; Compile a syntaxed file
(define (scheme-runtime/compile-bin-file . args)
  (error "scheme-runtime/compile-bin-file not currently supported")
  (cond ((version-7.2?) 
	 (if (environment-bound? system-global-environment 'compile-bin-file)
	     compile-bin-file
	     (lambda (filename)
	       (display "Compile not loaded, unable to compile: ")
	       (display filename)
	       (newline))))
	(else (version-error)))
  )


(define (scheme-runtime/file-modification-time file)
  ;; Return a 0 rather than null if file not present
  (let ((t (file-modification-time file)))
    (if (null? t) 0 t)))

;;---------------------------------------------------
;; Start up Scheme+
(scheme+)
