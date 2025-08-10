package net.cmr.alchemycompany.game;

public class Effects {
    
    public enum AttackType {
        NORMAL,
        CORROSION,
        ;

        @Override
        public String toString() {
            return "ATTACKTYPE."+name();
        }
    }

    // not likely to be implemented
    public enum PotionType {

    }

}
