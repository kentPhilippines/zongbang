package test.number;


import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.springframework.data.redis.core.ZSetOperations;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class rsa {


    public static void main(String[] args) {


        List<Double> l = new ArrayList<>();
        l.add(8.0);
        l.add(9.0);
        l.add(10.0);


        Collections.sort(l, new Comparator<Double>() {
            @Override
            public int compare(Double o1, Double o2) {
                return  o1 < o2 ? -1 : 1 ;
            }
        });

        for (Double a : l){
            System.out.println(a);
        }

    }
}
