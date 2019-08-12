import tester.Tester;

// Class for a course
class Course {
  String name;
  IList<Course> prereqs;

  // Constructor for creating an instance of a course
  Course(String name, IList<Course> prereqs) {
    this.name = name;
    this.prereqs = prereqs;
  }

  // returns the deepest path length of this course
  int getDeepestPathLength() {
    return new DeepestPathLength().apply(this.prereqs);
  }

  // returns whether this course has a prerequisite with the given name.
  boolean hasPrereq(String name) {
    return new OrMap<Course>(new HasPrereq(name)).apply(this.prereqs);
  }
}

// interface an IFunc
interface IFunc<A, R> {
  R apply(A arg);
}

// interface for IListVisitor which extends an IFunc
interface IListVisitor<T, R> extends IFunc<IList<T>, R> {
  // visitor for MtList
  R visitMt(MtList<T> mt);

  // visitor for ConsList
  R visitCons(ConsList<T> cons);
}

// OrMap class of type t that implements an IListVisitor of Type T that returns a boolean.
class OrMap<T> implements IListVisitor<T, Boolean> {
  IPred<T> pred;

  // Constructor for OrMap
  public OrMap(IPred<T> pred) {
    this.pred = pred;
  }

  // apply function for OrMap. Returns a boolean
  public Boolean apply(IList<T> args) {
    return args.accept(this);
  }

  // base case method for OrMap. Will return false.
  public Boolean visitMt(MtList<T> mt) {
    return false;
  }

  // function takes in a ConsList<T> and applies the predicate to the first
  // element and then recursively calls on the rest of the list
  public Boolean visitCons(ConsList<T> cons) {
    return this.pred.apply(cons.first) || this.apply(cons.rest);
  }

}

// interface for IPred<X> which is an IFunc<X, Boolean>
interface IPred<X> extends IFunc<X, Boolean> {

}

// class HasPrereq implements a predicate of type Course
class HasPrereq implements IPred<Course> {
  String name;

  // Constructor for HasPrereq
  public HasPrereq(String name) {
    this.name = name;
  }

  // apply function for HasPrereq. Returns whether this name equals the given
  // course name.
  public Boolean apply(Course arg) {
    return arg.name.equals(this.name);
  }

}

// class DeepestPathLength which implements  IFunc<Course, Integer>
class DeepestPathLength implements IListVisitor<Course, Integer> {

  // apply function that takes in a course as an argument and returns an Integer
  public Integer apply(IList<Course> arg) {
    return arg.accept(this);
  }

  // visitMt function: Takes in an MtList<Course> and returns 0
  public Integer visitMt(MtList<Course> mt) {
    return 0;
  }

  // visitCons function: Takes in an MtList<Course> and returns the deepest path
  // length
  public Integer visitCons(ConsList<Course> cons) {
    return Math.max(cons.first.getDeepestPathLength() + 1, this.apply(cons.rest));
  }

}

// interface for a IList of T
interface IList<T> {
  // returns the result of applying the given visitor to this list
  <R> R accept(IListVisitor<T, R> visitor);

}

// MtList class of type T which implements an IList
class MtList<T> implements IList<T> {
  // accept function which calls the visitMt function
  public <R> R accept(IListVisitor<T, R> visitor) {
    return visitor.visitMt(this);
  }

}

// class for a ConsList of T that implements a List of T
class ConsList<T> implements IList<T> {
  T first;
  IList<T> rest;

  // Constructor for ConsList of T
  ConsList(T first, IList<T> rest) {
    this.first = first;
    this.rest = rest;
  }

  // Accept function for this ConsList that accepts an IListVisitor and calls the
  // visitConsMethod
  public <R> R accept(IListVisitor<T, R> visitor) {
    return visitor.visitCons(this);
  }

}

// Examples Class for Courses
class ExamplesCourses {
  Course fundies1 = new Course("Fundies 1", new MtList<Course>());
  IList<Course> mt = new MtList<Course>();
  IList<Course> list1 = new ConsList<Course>(fundies1, mt);
  Course fundies2 = new Course("Fundies 2", this.list1);
  IList<Course> list2 = new ConsList<Course>(fundies2, list1);
  Course ood = new Course("ood", this.list2);
  Course algo = new Course("algo", this.list2);
  IList<Course> list3 = new ConsList<Course>(ood, list2);
  Course ai = new Course("ai", this.list3);

  //test for apply in IFunc<A, R>
  boolean testApply(Tester t) {
    return t.checkExpect(new DeepestPathLength().apply(list1), 1)
        && t.checkExpect(new DeepestPathLength().apply(list2), 2)
        && t.checkExpect(new DeepestPathLength().apply(list3), 3);
  }

  // test for IListVisitor in DeepestPathLength class
  boolean testIListVisitor(Tester t) {
    return t.checkExpect(new DeepestPathLength().visitMt(new MtList<Course>()), 0)
        && t.checkExpect(
            new DeepestPathLength().visitCons(new ConsList<Course>(this.fundies1, this.mt)), 1)
        && t.checkExpect(
            new DeepestPathLength().visitCons(new ConsList<Course>(this.ood, this.list2)), 3);

  }

  // test apply for OrMap class
  boolean testApplyOrMap(Tester t) {
    return t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 1")).apply(list1), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 1")).apply(list2), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 1")).apply(list3), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 2")).apply(list3), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 2")).apply(list1), false)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("ood")).apply(list2), false)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("asdf")).apply(list3), false);
  }

  // test visit methods for OrMap Class
  boolean testVisitOrMap(Tester t) {
    return t.checkExpect(
        new OrMap<Course>(new HasPrereq("Fundies 1")).visitMt(new MtList<Course>()), false)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 1"))
            .visitCons(new ConsList<Course>(this.fundies1, this.mt)), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 1"))
            .visitCons(new ConsList<Course>(this.fundies2, this.list1)), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 2"))
            .visitCons(new ConsList<Course>(this.ood, this.list2)), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("Fundies 2"))
            .visitCons(new ConsList<Course>(this.fundies1, this.list2)), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("ood"))
            .visitCons(new ConsList<Course>(this.ood, this.list2)), true)
        && t.checkExpect(new OrMap<Course>(new HasPrereq("asdf"))
            .visitCons(new ConsList<Course>(this.ood, this.list2)), false);
  }

  // Tests for apply function in HasPrereq Class
  boolean testHasPrereqApply(Tester t) {
    return t.checkExpect(new HasPrereq("Fundies 1").apply(this.fundies2), false)
        && t.checkExpect(new HasPrereq("Fundies 2").apply(this.fundies1), false)
        && t.checkExpect(new HasPrereq("ood").apply(this.ai), false)
        && t.checkExpect(new HasPrereq("algo").apply(this.algo), true)
        && t.checkExpect(new HasPrereq("ai").apply(this.ood), false)
        && t.checkExpect(new HasPrereq("Fundies 2").apply(this.fundies2), true)
        && t.checkExpect(new HasPrereq("ood").apply(this.fundies2), false)
        && t.checkExpect(new HasPrereq("algo").apply(this.ood), false)
        && t.checkExpect(new HasPrereq("ai").apply(this.fundies1), false);
  }

  // Test for getDeepestPathLength Function
  boolean testDeepestPathLength(Tester t) {

    return t.checkExpect(this.fundies1.getDeepestPathLength(), 0)
        && t.checkExpect(this.fundies2.getDeepestPathLength(), 1)
        && t.checkExpect(this.ood.getDeepestPathLength(), 2)
        && t.checkExpect(this.algo.getDeepestPathLength(), 2)
        && t.checkExpect(this.ai.getDeepestPathLength(), 3);

  }

  // Tests for hasPrereq function in Course
  boolean testHasPrereq(Tester t) {
    return t.checkExpect(this.fundies1.hasPrereq("Fundies 1"), false)
        && t.checkExpect(this.fundies1.hasPrereq("Fundies 2"), false)
        && t.checkExpect(this.fundies2.hasPrereq("Fundies 1"), true)
        && t.checkExpect(this.fundies2.hasPrereq("Fundies 2"), false)
        && t.checkExpect(this.fundies2.hasPrereq("ood"), false)
        && t.checkExpect(this.ood.hasPrereq("Fundies 1"), true)
        && t.checkExpect(this.ood.hasPrereq("Fundies 2"), true)
        && t.checkExpect(this.ood.hasPrereq("asdf"), false)
        && t.checkExpect(this.algo.hasPrereq("Fundies 2"), true)
        && t.checkExpect(this.algo.hasPrereq("ood"), false)
        && t.checkExpect(this.ai.hasPrereq("ood"), true);
  }

  // Tests for accept function in IList interface
  boolean testAcceptIList(Tester t) {
    return t.checkExpect(this.list1.accept(new OrMap<Course>(new HasPrereq("Fundies 1"))), true)
        && t.checkExpect(this.list2.accept(new OrMap<Course>(new HasPrereq("Fundies 1"))), true)
        && t.checkExpect(this.list3.accept(new OrMap<Course>(new HasPrereq("Fundies 1"))), true)
        && t.checkExpect(this.list1.accept(new OrMap<Course>(new HasPrereq("Fundies 2"))), false)
        && t.checkExpect(this.list2.accept(new OrMap<Course>(new HasPrereq("Fundies 2"))), true)
        && t.checkExpect(this.list3.accept(new OrMap<Course>(new HasPrereq("ood"))), true)
        && t.checkExpect(this.mt.accept(new OrMap<Course>(new HasPrereq("Fundies 2"))), false)
        && t.checkExpect(this.mt.accept(new DeepestPathLength()), 0)
        && t.checkExpect(this.list1.accept(new DeepestPathLength()), 1);
  }

}