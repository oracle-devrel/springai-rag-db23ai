drop table if exists emp purge;
drop table if exists dept purge;
drop view if exists department_dv;


create table dept (
  deptno number(2) constraint pk_dept primary key,
  dname varchar2(14),
  loc varchar2(13)
) ;

create table emp (
  empno number(4) constraint pk_emp primary key,
  ename varchar2(10),
  job varchar2(9),
  mgr number(4),
  hiredate date,
  sal number(7,2),
  comm number(7,2),
  deptno number(2) constraint fk_deptno references dept
);

create index emp_dept_fk_i on emp(deptno);

insert into dept values (10,'ACCOUNTING','NEW YORK');
insert into dept values (20,'RESEARCH','DALLAS');
insert into dept values (30,'SALES','CHICAGO');
insert into dept values (40,'OPERATIONS','BOSTON');

insert into dept values (50,'STORAGE','LOS ANGELES');
insert into dept values (60,'HEADQUARTER','PHOENIX');
insert into dept values (70,'LOGISTICS','SAN DIEGO');
insert into dept values (80,'RETAIL','SEATTLE');

insert into emp values (7369,'SMITH','CLERK',7902,to_date('17-12-1980','dd-mm-yyyy'),800,null,20);
insert into emp values (7499,'ALLEN','SALESMAN',7698,to_date('20-2-1981','dd-mm-yyyy'),1600,300,30);
insert into emp values (7521,'WARD','SALESMAN',7698,to_date('22-2-1981','dd-mm-yyyy'),1250,500,30);
insert into emp values (7566,'JONES','MANAGER',7839,to_date('2-4-1981','dd-mm-yyyy'),2975,null,20);
insert into emp values (7654,'MARTIN','SALESMAN',7698,to_date('28-9-1981','dd-mm-yyyy'),1250,1400,30);
insert into emp values (7698,'BLAKE','MANAGER',7839,to_date('1-5-1981','dd-mm-yyyy'),2850,null,30);
insert into emp values (7782,'CLARK','MANAGER',7839,to_date('9-6-1981','dd-mm-yyyy'),2450,null,10);
insert into emp values (7788,'SCOTT','ANALYST',7566,to_date('13-JUL-87','dd-mm-rr')-85,3000,null,20);
insert into emp values (7839,'KING','PRESIDENT',null,to_date('17-11-1981','dd-mm-yyyy'),5000,null,10);
insert into emp values (7844,'TURNER','SALESMAN',7698,to_date('8-9-1981','dd-mm-yyyy'),1500,0,30);
insert into emp values (7876,'ADAMS','CLERK',7788,to_date('13-JUL-87', 'dd-mm-rr')-51,1100,null,20);
insert into emp values (7900,'JAMES','CLERK',7698,to_date('3-12-1981','dd-mm-yyyy'),950,null,30);
insert into emp values (7902,'FORD','ANALYST',7566,to_date('3-12-1981','dd-mm-yyyy'),3000,null,20);
insert into emp values (7934,'MILLER','CLERK',7782,to_date('23-1-1982','dd-mm-yyyy'),1300,null,10);

insert into emp values (7544,'MICHAEL','ANALYST',7698,to_date('8-9-1982','dd-mm-yyyy'),1500,0,50);
insert into emp values (7996,'HENRY','CLERK',7788,to_date('13-JUL-83', 'dd-mm-rr')-51,2100,null,60);
insert into emp values (7977,'MARK','CLERK',7698,to_date('3-12-1985','dd-mm-yyyy'),1950,null,60);
insert into emp values (7190,'ANDY','ANALYST',7566,to_date('3-12-1982','dd-mm-yyyy'),1000,null,70);
insert into emp values (7230,'DOUG','CLERK',7782,to_date('23-2-1984','dd-mm-yyyy'),2000,null,70);
insert into emp values (7276,'SANJAY','CLERK',7782,to_date('23-2-1984','dd-mm-yyyy'),1000,null,80);
commit;

create or replace json relational duality view department_dv as
select json {
             '_id' : d.deptno,
             'departmentName'   : d.dname,
             'location'         : d.loc,
             'employees' :
               [ select json {'employeeNumber' : e.empno,
                              'employeeName'   : e.ename,
                              'job'            : e.job,
                              'salary'         : e.sal}
                 from   emp e with insert update delete
                 where  d.deptno = e.deptno ]}
from dept d with insert update delete;
commit;
exit;