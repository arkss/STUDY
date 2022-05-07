package hellojpa;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

public class JpaMain {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");

        EntityManager em = emf.createEntityManager();

        EntityTransaction tx = em.getTransaction();
        tx.begin();

        List<Member> resultList = em.createQuery("select m from Member as m", Member.class)
                                    .getResultList();

        for (Member member : resultList) {
            System.out.println("member.name = " + member.getName());
        }

        tx.commit();

        em.close();
        emf.close();
    }
}
