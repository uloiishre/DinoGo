package com.dinogo.seller.repository;

//sysmsg-start，總共3次修改，第1次//
import java.util.Collection;
import org.springframework.data.jpa.repository.EntityGraph;
//sysmsg-end，總共3次修改，第1次//
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.dinogo.seller.entity.Seller;

public interface SellerRepository extends JpaRepository<Seller, Integer> {
    Optional<Seller> findBySellerId(Integer sellerId);
    Optional<Seller> findByMember_MemberId(Integer memberId);

    //sysmsg-start，總共3次修改，第2次//
    /** OA 廣播一次對應會員與商家，並預載 Member 的信箱及通知偏好。 */
    @EntityGraph(attributePaths = "member")
    List<Seller> findByMember_MemberIdIn(Collection<Integer> memberIds);
    //sysmsg-end，總共3次修改，第2次//

    //sysmsg-start，總共3次修改，第3次//
    /** OA 商家廣播使用，預載 Member 以取得信箱與通知偏好。 */
    @EntityGraph(attributePaths = "member")
    List<Seller> findAllByStatusIgnoreCase(String status);
    //sysmsg-end，總共3次修改，第3次//

    boolean existsBySellerId(Integer sellerId);
    boolean existsBySellerIdAndStatus(Integer sellerId, String status);
    boolean existsByMember_MemberId(Integer memberId);
    List<Seller> findByStoreNameContainingAndStatus(String keyword, String status);
}
